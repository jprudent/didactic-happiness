(ns cheap-hate.console-screen
  "An implementation of Screen on console.
  The console should support ANSI escape sequence"
  (:require [cheap-hate.cursor :as curse]
            [cheap-hate.core :as core]))

(defn- print! [x y c]
  (print (str (curse/locate (inc x) (inc y)) c)))                               ;; todo why (0,0) doesn't work?

(defn- draw-pixel! [x y pixel]
  (print! x y (if (pos? pixel) \u2588 \space)))

(defn- hex-3 [n] (format "%03X" (or n 0xFF)))

(defn- print-register [r v x y]
  (print (str (curse/locate x y) r))
  (print (str (curse/locate x (inc y))
              (hex-3 v))))
(defrecord ConsoleScreen [previous-machine]

  core/Screen
  (print-screen [this machine last-instruction]                                 ;; could we optimize by printing a single string ?
    (when (= :draw (first last-instruction))
      (dotimes [x 64]
        (dotimes [y 32]
          (let [previous-pixel (core/get-pixel previous-machine x y)
                current-pixel  (core/get-pixel machine x y)]
            (when (not= previous-pixel current-pixel)
              (draw-pixel! x y current-pixel))))))

    (dotimes [x 16] (print-register x (core/get-register machine x)
                                    (inc (* 4 x)) 33))
    (print-register "PC" (core/get-pc machine) 1 35)
    (print-register "I" (core/get-i machine) 5 35)
    (print-register "K" (core/get-keyboard machine) 9 35)
    (assoc this :previous-machine machine)))




