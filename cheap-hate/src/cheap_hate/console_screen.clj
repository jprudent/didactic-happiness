(ns cheap-hate.console-screen
  (:require [cheap-hate.cursor :as curse]
            [cheap-hate.core :as c8]))

(defn print! [x y c]
  (print (str (curse/locate (inc x) (inc y)) c)))                               ;; todo why (0,0) doesn't work?

(defn draw-pixel! [x y pixel]
  ((partial print! x y) (if (pos? pixel) \u2588 \space)))

(def hex #(Integer/toHexString %))

(defn print-reg [r v x y]
  (print (str (curse/locate x y)
              (if (number? r) (hex r) r)))
  (print (str (curse/locate x (inc y))
                    (hex v))))
(defrecord ConsoleScreen []
  c8/Screen
  (print-screen [_ machine last-instruction]

    (when (= :draw (first last-instruction))
      (dotimes [x 64]
        (dotimes [y 32]
          (do (draw-pixel! x y (c8/get-pixel machine x y))))))

    (dotimes [x 12] (print-reg x (c8/get-register machine x) (* 3 (inc x)) 33))
    (print-reg "PC" (c8/get-pc machine) (* 3 13) 33)
    (print-reg "I" (c8/get-i machine) (* 3 14) 33)))




