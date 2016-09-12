// one file contains parts from multiple langs; a base lang, and other
// langs which are identified by special comments (%%scala eg);
// multiple versions of the file are available (eg x.tex, x.md,
// x.scala); this script converts between them

// FIXME only produce targets that already exist?


object Multilang {

  // a line is special if it starts: //abc, where abc is some
  // extension we are interested in; the // is the comment syntax of
  // hte source file
  case class Lang(val start : String, val end : String, val ext : String)

  var langs = List(
    Lang("//","","md"),
    Lang("%%","","tex"),
    Lang("//","XX","scala"))

  var exts = langs.map(_.ext)

  def is_special(l:Lang, s:String) = {
    s.startsWith(l.start) && (
      exts.exists( ext => s.substring(l.start.length).startsWith(ext)))
  }

  // if it is special, what is the extension?
  def special_ext(l:Lang,s:String) = {
    exts.find(ext => s.substring(l.start.length).startsWith(ext)).get
  }

  // assume a special comment, //ext ...//, return ext ...
  def drop_start_end(from:Lang,s:String) = {
    s.substring(from.start.length).dropRight(from.end.length)
  }

  // assuming a special comment //abc... //, return ...
  def drop_comment(from:Lang,s:String) = {
    val ext = special_ext(from,s)
    val m = (from.start+ext+" ").length // assumes ext followed by space
    val n = (from.end).length
    s.substring(m).dropRight(n)
  }

  // assuming s is special, change comment syntax
  def change_lang(from:Lang, to:Lang, s:String) = {
    val s2 = drop_start_end(from,s)
    to.start+s2+to.end
  }

  def add_comment(from:Lang,to:Lang,s:String) = {
    to.start+from.ext+" "+s+to.end
  }

  def main(fn:String, from:Lang, to:Lang) = {
    val cf = from.start
    val ct = to.start

    val ss = _root_.scala.io.Source.fromFile(s"${fn}.${from.ext}")
    var t : List[String] = List()
    for(s <- ss.getLines) {
      is_special(from,s) match {
        case true => {
          val ext = special_ext(from,s)
          if (ext == to.ext) {
            // if s starts with the target comment, drop it
            t = (drop_comment(from,s))::t
          } else {
            // otherwise, if a special comment, pass through unchanged (modulo cf ct)
            t = (change_lang(from,to,s)) ::t
          }
        }
        case false => {
          // otherwise guard the line with the ct and from ext
          t = (add_comment(from,to,s)) :: t 
        }
      }
    }
    val t2 = t.reverse.mkString("\n")
    // println(t2)
    // write t to target file
    Script_common.write_file(s"${fn}.${to.ext}",t2)
  }

  def main(args:Array[String]) : Unit = {
    import Script_common._

    // first, check if there is a .multilang file present

    var base_to_exts : Map[String,List[String]] = Map()
    read_file(".multilang") match {
      case None => ()
      case Some(s) => {
        import upickle.default._
        val (ls,m) = read[(List[Lang],Map[String,List[String]])](s)
        langs = ls
        exts = langs.map(_.ext)
        // FIXME what to do with m?
        base_to_exts = m
        println("read .multilang ")
      }
    }

    val fn=args(0)

    // base is the basename of the file that has changed; ext it the
    // extension
    val (base,ext) = split_filename(fn)

    // which languages are we working with?
    val from=langs.find(l => l.ext == ext).get
    val tos = langs.filter(l => { (l != from) && base_to_exts(base).contains(l.ext) })
    //println(s"tos : $tos")
    // for each (from,to), run main
    tos.map(to => main(base,from,to))
  }

}


