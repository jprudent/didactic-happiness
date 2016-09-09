(ns cheap-hate.decompiler
  (:require [cheap-hate.romloader :as rom]
            [cheap-hate.parser :as parser]
            [cheap-hate.bits-util :as bits]))

(defn hex4 [n] (format "0x%04X", n))
(defn hex2 [n] (format "0x%02X", n))
(defn hex-i [part] (if (number? part) (symbol (hex2 part)) part))

(defn decompile [rom-file]
  (->> (rom/load-rom rom-file)
       (partition 2)
       (map (fn [[b1 b2]] (bits/concat-bytes b1 b2)))
       (map (fn [address opcode]
              [(str "@" (hex4 address))
               (hex4 opcode)
               (map hex-i (parser/opcode->instruction opcode))])
            (range 0x200 0x1000 2))))