(ns cryptopals.aes-detect)

(defn most-repeated-block
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
  (->> (map most-repeated-block seq-bytes)
       (map vector seq-bytes)
       (sort-by second)
       last)
  )
