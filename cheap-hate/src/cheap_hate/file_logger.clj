(ns cheap-hate.file-logger
  (:require [cheap-hate.core :refer :all]
            [cheap-hate.parser :as parser]))

(defn hex4 [n] (format "0x%04X", n))
(defn hex2 [n] (format "0x%02X", n))
(defn hex1 [n] (format "0x%1X", n))

(defn hex-i [part]
  (if (number? part) (symbol (hex2 part)) part))

(defrecord FileLogger [file-name]
  FlightRecorder
  (record [_ machine opcode]
    (spit file-name
          (-> (map #(vector (symbol (hex1 %)) (symbol (hex4 (get-register machine %))))
                   (range 16))
              (conj (map hex-i ["I" (get-i machine)]))
              (conj (map hex-i ["PC" (get-pc machine)]))
              (conj [(hex4 opcode)
                     (map hex-i (parser/opcode->instruction opcode))]))
          :append true)
    (spit file-name "\n" :append true)))
