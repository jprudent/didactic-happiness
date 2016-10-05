(ns cryptopals.aes-detect)

(defn frequency-of-most-repeated-block
  [bytes]
  {:pre [(= 0 (mod (count bytes) 16))]}
  (->> (partition 16 bytes)
       (frequencies)
       (sort-by second)
       reverse
       first
       second))

(defn detect-aes-ecb
  [seq-bytes]
  (->> (map frequency-of-most-repeated-block seq-bytes)
       (map vector seq-bytes)
       (sort-by second)
       last))

(defn ecb-mode? [ciphered-bytes]
  (> (frequency-of-most-repeated-block ciphered-bytes) 1))


