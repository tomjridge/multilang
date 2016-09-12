import scala.sys.process._
import java.io._

import scala.collection.immutable.StringOps

object Script_common {

  /******************** scala lib */

  def debug(s:String) = {}

  implicit class Any_with_rev_app[B](x:B) {
    def |>[A](f:B => A) = {f(x)}
  }

  def failwith(s:String) : Nothing = { throw new Exception(s) }

  def s_to_lines(s:String) : List[String] = s.linesIterator.toList // or split on whitespace?

  val whitespace = "\\s+"

  import scala.language.implicitConversions
  def s_to_ss(s:String) : List[String] = s.split(whitespace).toList

  def remove_blank_lines(x:List[String]) = x.filter( s => 
    !(s.matches(whitespace) || s == ""))

  implicit def s_to_list_string(s:String): List[String] = s|>s_to_ss|>remove_blank_lines

  def read_file(fn:String) : Option[String] = {
    try { Some(scala.io.Source.fromFile(fn).mkString) } 
    catch { case (_:Exception) => None }
  }

  def write_file(fn:String,s:String) = {
    // http://stackoverflow.com/questions/6879427/scala-write-string-to-file-in-one-statement ; a real horror
    import java.nio.file.{Paths, Files}
    import java.nio.charset.StandardCharsets
    Files.write(Paths.get(fn), s.getBytes(StandardCharsets.UTF_8))
  }

  var tmp_file_index = 0
  def tmp_file() : String = {
    tmp_file_index = tmp_file_index+1
    s"/tmp/scala_tmp$tmp_file_index"
  }

  // http://stackoverflow.com/questions/18160045/fastest-serialization-deserialization-of-scala-case-classes
  def serialize[T <: Serializable](obj: T): Array[Byte] = {
    val byteOut = new ByteArrayOutputStream()
    val objOut = new ObjectOutputStream(byteOut)
    objOut.writeObject(obj)
    objOut.close()
    byteOut.close()
    byteOut.toByteArray
  }

  def deserialize[T <: Serializable](bytes: Array[Byte]): T = {
    val byteIn = new ByteArrayInputStream(bytes)
    val objIn = new ObjectInputStream(byteIn)
    val obj = objIn.readObject().asInstanceOf[T]
    byteIn.close()
    objIn.close()
    obj
  }


  /******************** script lib */

  // we need an implicit conversion from string to file
  import scala.language.implicitConversions

  implicit def s_to_f(s:String)(implicit c: Context) : File = {
    val wd = if (c.wd.endsWith("/")) c.wd else c.wd+"/" // note that we always add a /
    if(s.startsWith("/")) (new File(s))
    else (new File(wd+s))
  }

  def suffix(s:String) = (s:StringOps).drop(s.lastIndexOf("."))

  // abc.ext => (abc,ext)
  def split_filename(s:String) = {
    val n = s.lastIndexOf(".")
    ((s:StringOps).take(n),(s:StringOps).drop(n+1))
  }

  implicit class My_rich_string(s:String) {
    def from(a:String) = s.stripSuffix(a)
    def to(b:String) = s+b
    def /(t:String) = s+"/"+t
    def ext() = suffix(s)
  }


  case class Context(wd:String=".")

  def execb(echo:Boolean,s:String)(implicit c:Context) : List[String] = {
    if(echo) println("#exec: "+c.wd+"\n"+s)
    // println(s)
    val result : String = { Process(s,new File(c.wd)).!! }
    debug("#exec result: \n"+result)
    result |> s_to_lines
  }

  def exec(s:String)(implicit c:Context) : List[String] = execb(true,s)(c)

  // no echo
  def execq(s:String)(implicit c:Context) : List[String] = execb(false,s)(c)


  def execs(t:String)(implicit c:Context) : List[String] = {
    val ss = s_to_lines(t) |> remove_blank_lines
    ss.flatMap(f => exec(f)(c))
  }

  // true if any dst does not exist, or some src is newer than some dst
  def newer(src:List[String],dst:List[String])(implicit c:Context) : Boolean = {
    // check if all dst exist
    val not_exists = dst.find(f => !(f:File).exists)
    not_exists match {
      case Some(x) => { debug(s"newer: $x not present"); true}
      case None => {
        val dst_min = {Long.MaxValue :: (dst.map(f => (f:File).lastModified())) }.min
        val src_newer = src.find(f => (f:File).lastModified() > dst_min)
        src_newer match {
          case None => false
          case Some(x) => { debug(s"newer: src $x is newer"); true }
        }
      }
    }
  }

}

