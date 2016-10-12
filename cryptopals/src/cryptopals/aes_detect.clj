(ns cryptopals.aes-detect
  (:require [cryptopals.aes :refer :all]
            [cryptopals.ascii-bytes :refer :all]))

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
  "returns the block size of the oracle
  Limitation : This only works if oracle is prepending input."
  [oracle]
  (first (let [ciphered       (oracle (repeat 1000 0xAA))
               starting-index (quot (count ciphered) 3)
               ciphered       (drop starting-index ciphered)]
           (filter (complement nil?)
                   (for [i (range 4 100)
                         :let [chunk    (take i ciphered)
                               ciphered (drop i ciphered)]]
                     (when (= (take i ciphered) chunk) i))))))

(defn make-dic
  "Make a dictionary of every possible (conj plain-bytes byte)"
  [cipher plain-bytes dic-key-size]
  {:pre [(vector? plain-bytes)]}
  (->> (range 256)
       (map (partial conj plain-bytes))
       (pmap #(vector (take dic-key-size (cipher %)) (last %)))
       (into {})))

(def rand-byte (partial rand-int 256))
(defn random-bytes [n] (repeatedly n rand-byte))
(def random-key (partial random-bytes (* block-size word-size)))


(defn index-of-consecutive-identical-blocks [bytes block-size]
  (let [blocks (partition block-size bytes)]
    (->> (map vector blocks (concat [nil] blocks) (range))
         (some (fn [[block last-block block-index]]
                 (when (= block last-block) (dec block-index)))))))

(defn pad-before
  "If you pass a string that have a size computed by this fonction,
   under the hood, the oracle will cipher a message that have a size
   multiple of block-size. That also means that the last block is
   entirely a padding block (assuming that PKCS7 is used).
   This only works if the oracle is a padding oracle that prepends a constant."
  [oracle block-size]
  (let [block (random-bytes block-size)]
    (some
      (fn [[nb-padding ciphered]]
        (when-let [block-index (index-of-consecutive-identical-blocks ciphered block-size)]
          [block-index nb-padding]))
      (for [nb-padding (range 0 (inc block-size))
            :let [padding  (random-bytes nb-padding)
                  ciphered (oracle (concat padding block block))]]
        [nb-padding ciphered]))))

(defn crack-one-byte-ecb [oracle known-secret-bytes block-size prelude-size]
  (let [a-byte             0xAA
        padding-size       (- block-size (mod (count known-secret-bytes) block-size) 1)
        padding-bytes      (repeat padding-size a-byte)
        crafted-block-size (+ padding-size
                              prelude-size
                              1
                              (count known-secret-bytes))
        crafted-block      (take crafted-block-size
                                 (oracle padding-bytes))
        dic                (make-dic oracle (vec (concat padding-bytes known-secret-bytes))
                                     crafted-block-size)]
    (get dic crafted-block)))

(defn one-byte-at-a-time-attack
  ([oracle]
   (when (ecb-mode? (oracle (repeat 1000 0xAA)))
     (let [block-size   (detect-block-size oracle)
           [block-index prelude-padding-size] (pad-before oracle block-size)
           oracle       (fn [plain-bytes]
                          (oracle (concat
                                    (repeat prelude-padding-size 0xBB)
                                    plain-bytes)))
           prelude-size (* block-index block-size)]
       (one-byte-at-a-time-attack oracle block-size prelude-size))))
  ([oracle block-size prelude-size]
   (loop [acc []]
     (if-let [cracked-byte (crack-one-byte-ecb oracle acc block-size prelude-size)]
       (do (println (char cracked-byte) cracked-byte)
           (recur (conj acc cracked-byte)))
       (drop-last acc)))))