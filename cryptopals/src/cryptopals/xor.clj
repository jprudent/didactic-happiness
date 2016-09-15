(ns cryptopals.xor
  (:require
    [cryptopals.ascii-bytes :refer :all]))

(defn xor [bytes1 bytes2]
  (map bit-xor bytes1 bytes2))

(defn by-second-descending [[_ v1] [_ v2]] (> v1 v2))

(defn- mean-frequencies
  "Return the frequency (between 0 and 1) of each byte"
  [bytes]
  (->> (frequencies bytes)
       (map (fn [[k v]] [k (/ v (count bytes))]))
       (into {})
       (sort by-second-descending)))

(def most-frequent-english-letters (map byte #{\space \e \t \a \o}))

(defn weight
  "found-most-freq is a seq of byte ordered by popularity decreasing
  reference-most-freq has the same type"
  [found-most-freq reference-most-freq]
  (count
    (clojure.set/intersection
      (set reference-most-freq)
      (set (take (count reference-most-freq) found-most-freq)))))

(defn crack-single-byte-xor-key [xored-bytes]
  (let [most-frequent-byte (ffirst (mean-frequencies xored-bytes))]
    (sort by-second-descending
          (for [letter most-frequent-english-letters
                :let [probable-key             (bit-xor most-frequent-byte letter)
                      deciphered               (xor (repeat probable-key) xored-bytes)
                      frequency-deciphered     (mean-frequencies deciphered)
                      most-frequent-deciphered (map first frequency-deciphered)
                      deciphered-weight        (weight most-frequent-deciphered most-frequent-english-letters)]]
            [(bytes->ascii-string deciphered) deciphered-weight probable-key]))))

(defn xor-text [text key]
  (let [repeat-key (cycle (ascii-string->bytes key))]
    (-> (ascii-string->bytes text)
        (xor repeat-key)
        bytes->hexstring)))

(defn count-bits [n]
  (loop [acc 0, n n]
    (if (zero? n)
      acc
      (recur (+ (bit-and 2r1 n) acc) (bit-shift-right n 1)))))

(defn hamming-distance [bytes1 bytes2]
  (reduce + 0 (map (comp count-bits bit-xor) bytes1 bytes2)))

(defn crack-repeating-xor-key [xored-bytes]
  (for [keysize (range 2 40)
        :let [[segment1 segment2 & _] (partition keysize xored-bytes)
              distance (/ (hamming-distance segment1 segment2) keysize)]]
    [keysize distance]))