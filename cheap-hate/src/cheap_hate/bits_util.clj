(ns cheap-hate.bits-util
  "This namspace contains utilities for bitwise manipulations.")

(defn- power-of-2 [exp] (bit-shift-left 1 exp))
(defn- mask-of-size [size] (dec (power-of-2 size)))
(defn nth-word
  "returns the nth word in x, 0 being the righmost position.
  The word size is specified in bits

  (map (fn [nth] (nth-word 12 nth 0xABCD)) [0 1 2 3])
  => (0xBCD 0xA 0 0)"
  [word-size nth x]
  (let [bits   (* nth word-size)
        mask   (bit-shift-left (mask-of-size word-size) bits)
        masked (bit-and x mask)]
    (bit-shift-right masked bits)))

(def lowest-byte (partial nth-word 8 0))
(def two-lowest-bytes (partial nth-word 16 0))
(def lowest-bit (partial nth-word 1 0))
(def highest-bit (partial nth-word 1 7))
(defn bit-at [bit-num byte] (nth-word 1 (- 7 bit-num) byte))


(defn concat-bytes [b1 b2] (bit-or (bit-shift-left b1 8) b2))