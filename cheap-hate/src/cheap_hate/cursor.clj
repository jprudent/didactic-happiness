(ns cheap-hate.cursor
  "small namespace to print on term using ")

;; see http://ascii-table.com/ansi-escape-sequences.php

(def ^:static esc "\u001B[")
(def ^:static sep ";")

(defn locate
  "set cursor at (x,y)"
  [x y]
  (str esc y sep x "H"))

(def home (partial locate 0 0))

(def x (atom 0))
(def y (atom 0))
(def v (atom 0))
#_(while true
  (do
    (print (str (locate (mod @x 5) 0)) (mod @v 2))
    (swap! x inc)
    (swap! v inc)))