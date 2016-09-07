(ns cheap-hate.console-screen
  (:require [cheap-hate.cursor :as curse]
            [cheap-hate.core :as c8]))

(defn print! [x y c]
  (print (str (curse/locate (inc x) (inc y)) c))) ;; todo why (0,0) doesn't work?

(defn draw-pixel! [x y pixel]
  ((partial print! x y) (if (pos? pixel) \u2588 \space)))

(defrecord ConsoleScreen []
  c8/Screen
  (print-screen [_ machine]
    (dotimes [x 64]
      (dotimes [y 32]
        (do (draw-pixel! x y (c8/get-pixel machine x y)))))))




