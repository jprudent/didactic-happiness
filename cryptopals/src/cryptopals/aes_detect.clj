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
  "seq-bytes is a seq of ciphered bytes (several messages that have been
  ciphered). Returns a vector [ciphered-bytes n] of the most probable
  ciphered message with ECB where n is the frequency of the most repeated block
  of ciphered-bytes."
  [seq-bytes]
  (->> (map frequency-of-most-repeated-block seq-bytes)
       (map vector seq-bytes)
       (sort-by second)
       last))

(defn ecb-mode? [ciphered-bytes]
  (> (frequency-of-most-repeated-block ciphered-bytes) 1))

(defn detect-block-size
  "Given a cipher function, returns the block size of the cipher"
  [cipher])


