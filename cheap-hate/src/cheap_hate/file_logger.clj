(ns cheap-hate.file-logger
  "Implementation of a file based flight recorder"
  (:require [cheap-hate.core :as core]
            [cheap-hate.parser :as parser]))

(defn- hex4 [n] (format "0x%04X", n))
(defn- hex2 [n] (format "0x%02X", n))
(defn- hex1 [n] (format "0x%1X", n))

(defn- hex-i [part]
  (if (number? part) (symbol (hex2 part)) part))

(defrecord FileLogger [file-name]
  core/FlightRecorder
  (record [_ machine opcode]
    (spit file-name
          (-> (map #(vector (hex1 %) (hex4 (core/get-register machine %))) (range 16))
              (conj (map hex-i ["I" (core/get-i machine)]))
              (conj (map hex-i ["PC" (core/get-pc machine)]))
              (conj [(hex4 opcode)
                     (map hex-i (parser/opcode->instruction opcode))]))
          :append true)
    (spit file-name "\n" :append true)))
