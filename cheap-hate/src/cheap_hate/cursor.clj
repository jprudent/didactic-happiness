(ns cheap-hate.cursor
  "small namespace to print on term using ")

;; see http://ascii-table.com/ansi-escape-sequences.php

(def ^:static esc "\u001B[")
(def ^:static sep ";")

(defn locate
  "set cursor at (x,y)"
  [x y]
  (str esc x sep y "H"))

(def home (partial locate 0 0))


(println (locate 12 32 "foo"))