(ns cheap-hate.cursor
  "small namespace to print on term using ")

;; see http://ascii-table.com/ansi-escape-sequences.php

(def ^:static esc "\u001B[")
(def ^:static sep ";")

(defn locate
  "set cursor at (x,y)"
  [x y]
  (str esc y sep x "H"))

(def clear-screen (str esc "2J"))
