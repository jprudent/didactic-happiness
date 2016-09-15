(ns cryptopals.xor
  (:require
    [cryptopals.ascii-bytes :refer :all]))

(defn xor [bytes1 bytes2]
  (map bit-xor bytes1 bytes2))

(defn by-second-descending [[_ v1] [_ v2]] (> v1 v2))
(defn by-third-descending [[_ _ v1] [_ _ v2]] (> v1 v2))

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

(defn crack-single-byte-xor-key
  "Returns 5 solutions. Each solution is [deciphered-bytes key weight]
  The higher the weight, the higher is the probability of the deciphered-bytes
   being english."
  [xored-bytes]
  (let [most-frequent-byte (ffirst (mean-frequencies xored-bytes))]
    (sort by-second-descending
          (for [letter most-frequent-english-letters
                :let [probable-key             (bit-xor most-frequent-byte letter)
                      deciphered               (xor (repeat probable-key) xored-bytes)
                      frequency-deciphered     (mean-frequencies deciphered)
                      most-frequent-deciphered (map first frequency-deciphered)
                      deciphered-weight        (weight most-frequent-deciphered most-frequent-english-letters)]]
            [deciphered deciphered-weight probable-key]))))

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

(defn normalized-hamming-distance
  [bytes1 bytes2 keysize]
  (/ (hamming-distance bytes1 bytes2) keysize))

(defn mean [numbers]
  (/ (reduce + 0 numbers) (count numbers)))

(defn mean-distance [xored-bytes keysize]
  (let [partitions           (partition keysize xored-bytes)
        segment-pairs        (partition 2 partitions)
        normalized-distances (map (fn [[s1 s2]] (normalized-hamming-distance s1 s2 keysize)) segment-pairs)]
    (mean normalized-distances)))

(defn best-keysize [xored-bytes max-size]
  (->> (for [keysize (range 2 max-size)] [keysize (mean-distance xored-bytes keysize)])
       (sort by-second-descending)
       (take-last 5)
       (map first)))

(defn transpose [coll-of-coll]
  (apply map vector coll-of-coll))

(defn crack-repeating-xor-key
  "Returns 5 solutions. Each solution is [key-size deciphered-bytes key]"
  [xored-bytes]
  (map (fn [[keysize val]] [keysize
                            (->> (map second val)
                                 transpose
                                 (apply concat))
                            (->> (map #(nth % 2) val)
                                 (take keysize))])
       (group-by first
                 (for [key-size       (best-keysize xored-bytes 100)
                       vertical-chunk (transpose (partition key-size xored-bytes))
                       :let [cracked-vertical-chunk (first (crack-single-byte-xor-key vertical-chunk))]]
                   [key-size (first cracked-vertical-chunk) (nth cracked-vertical-chunk 2)]))))