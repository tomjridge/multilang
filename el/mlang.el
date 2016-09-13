;; -*- lexical-binding: t -*-

;;adoc = Multilang
;;adoc Author: Tom Ridge
;;adoc 
;;adoc 
;;adoc == Introduction
;;adoc 
;;adoc This is a simple script that allows you to edit multiple views of a
;;adoc single source document.
;;adoc 
;;adoc For example, suppose you want to mix source code in emacs lisp with
;;adoc markdown documentation. You write a single file - let's say `xxx.el` -
;;adoc with markdown "special multilang" comments. For example:
;;adoc 
;;adoc [source,lisp]
;;adoc --------------------------------------------------
;;adoc ;;md A **very common** function
;;adoc (defun f () (message "Hello world!"))
;;adoc 
;;adoc ;;md This is some *markdown* documentation
;;adoc --------------------------------------------------
;;adoc 
;;adoc You need to tell multilang about the comment syntaxes involved, and
;;adoc the files you wish to operate on. In a `.multilang` file, put
;;adoc something like:
;;adoc 
;;adoc [source,json]
;;adoc ----
;;adoc [
;;adoc   [{"start":"//","end":"","ext":"md"},
;;adoc    {"start":";;","end":"","ext":"el"}
;;adoc   ],
;;adoc   {"xxx":["md","el"]}
;;adoc ]
;;adoc ----
;;adoc 
;;adoc This tells multilang to process `xxx.md` to produce `xxx.el`, and vice
;;adoc versa. Then, when `xxx.md` changes, you run `Multilang xxx.md`, and
;;adoc similarly when `xxx.el` changes, run `Multilang xxx.el`.
;;adoc 
;;adoc To make this usable, you need to integrate with your editor. For
;;adoc example, emacs has a "after-save-hook" that enables you to run a
;;adoc command every time a file is saved. This can be used to execute
;;adoc `Multilang`. The following sections describe the emacs code to
;;adoc interface to `Multilang`.
;;adoc 
;;adoc 
;;adoc === Multilang Emacs Lisp code (mlang.el)
;;adoc 
;;adoc To use this code, call the function `multilang` on a buffer you wish
;;adoc to edit with multilang support. You also need `.multilang` set
;;adoc appropriately, and other related buffers should also have `multilang`
;;adoc called on them. Then, when saving the buffer, emacs will automatically
;;adoc run Multilang. 
;;adoc 
;;adoc 
;;adoc === Config // ----------------------------------------
;;adoc 
;;adoc `mlang/buffers`: a list of buffers that we revert after running the
;;adoc hook
;;adoc 
;;adoc `mlang/clear`: clear the buffer list (e.g. before working on another
;;adoc mlang doc)
;;adoc 
;;adoc `run-multilang`: how to run multilang on your system
;;adoc 
;;adoc [WARNING]
;;adoc You need to edit this function so that it runs multilang on your system
;;adoc 
;;adoc ----

(defvar mlang/buffers '())

(defun mlang/clear ()
  (interactive)
  (setq mlang/buffers '()))

;; how to call the Scala multilang executable on your system, with
;; argument filename fn; FIXME EDIT THIS FOR YOUR LOCAL INSTALLATION
(defun run-multilang (b)
  (interactive "bBuffer:")
  (shell-command 
   (format "runsc Multilang %s" ;; FIXME you want "multilang %s"
           (file-name-nondirectory (buffer-file-name (get-buffer b)))))) ;; fixme long-winded?

;;adoc ----
;;adoc 
;;adoc === Save hook // ----------------------------------------
;;adoc 
;;adoc On save, we run `Multilang` to propagate changes, and then revert all
;;adoc buffers in `mlang/buffers`. Note that we need the scala code
;;adoc `Multilang.scala`, and some way to run it (here we use a local `runsc`
;;adoc file, but you should use your own alternative).
;;adoc 
;;adoc `mlang/revert`: revert all buffers in `mlang/buffers`.
;;adoc 
;;adoc ----

(defun mlang/revert ()
  (interactive)
  (mapc ; revert relevant buffers - perhaps we want this in the scala?
   (lambda (x) 
     (with-current-buffer x 
       (revert-buffer nil t)))
   mlang/buffers))

(defun mlang/hook ()
  (interactive)
  (progn
    (message "mlang/hook")
    (run-multilang (current-buffer))
    (mlang/revert)
    (mlang/add-hook))) ;; having reverted, we need to re-add the hook

;;adoc ----
;;adoc 
;;adoc === Add hook to buffers // ---------------------------------------- 
;;adoc 
;;adoc ----

(defun mlang/add-hook ()
  (interactive)
  (mapc 
   (lambda (b)
     (with-current-buffer b 
       (add-hook 'after-save-hook 'mlang/hook nil t)))
   mlang/buffers))


;; add hook to current buffer
(defun multilang ()
  (interactive)
  (add-to-list 'mlang/buffers (current-buffer)) ;; add current buffer to mlang/buffers
  (add-hook 'after-save-hook 'mlang/hook nil t))

;;adoc ----
;;adoc 
;;adoc === Full source of `mlang.el` // ----------------------------------------
;;adoc 
;;adoc The full source is:
;;adoc 
;;adoc [source,lisp]
;;adoc ----
;;adoc include::mlang.el[]
;;adoc ----
