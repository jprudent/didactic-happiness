(ns cheap-hate.cursor
  "Small namespace to print on term using ANSI escape sequence.")

;; see http://ascii-table.com/ansi-escape-sequences.php

(def ^:static ^:private esc "\u001B[")
(def ^:static ^:private sep ";")

(defn locate
  "set cursor at (x,y)"
  [x y]
  (str esc y sep x "H"))
