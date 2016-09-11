(ns cheap-hate.console-screen
  (:require [cheap-hate.cursor :as curse]
            [cheap-hate.core :as c8]))

(defn print! [x y c]
  (print (str (curse/locate (inc x) (inc y)) c)))                               ;; todo why (0,0) doesn't work?

(defn draw-pixel! [x y pixel]
  ((partial print! x y) (if (pos? pixel) \u2588 \space)))

(def hex #(format "%03X" (or % 0xFF)))

(defn print-reg [r v x y]
  (print (str (curse/locate x y) r))
  (print (str (curse/locate x (inc y))
              (hex v))))
(defrecord ConsoleScreen [previous-machine]
  c8/Screen
  (print-screen [this machine last-instruction]

    (when (= :draw (first last-instruction))
      (dotimes [x 64]
        (dotimes [y 32]
          (let [previous-pixel (c8/get-pixel previous-machine x y)
                current-pixel  (c8/get-pixel machine x y)]
            (when (not= previous-pixel current-pixel)
              (draw-pixel! x y current-pixel))))))

    (dotimes [x 16] (print-reg x (c8/get-register machine x) (inc (* 4 x)) 33))
    (print-reg "PC" (c8/get-pc machine) 1 35)
    (print-reg "I" (c8/get-i machine) 5 35)
    (print-reg "K" (c8/get-keyboard machine) 9 35)
    (assoc this :previous-machine machine)))




