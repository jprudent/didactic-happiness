(ns repicene.bits
  (:require [repicene.schema :as s]))

(defn positive? [address]
  (zero? (bit-and address 2r10000000)))

(defn abs "(abs n) is the absolute value of n" [n]
  {:pre [(number? n)]}
  (if (neg? n) (- n) n))

(defn two-complement [word]
  {:pre  [(s/word? word)]
   :post [(<= (abs %) 127)]}
  (if (positive? word)
    word
    (* -1 (bit-and (inc (bit-not word)) 0xFF))))
