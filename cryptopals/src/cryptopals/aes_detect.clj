(ns cryptopals.aes-detect
  (:require [cryptopals.aes :refer :all]))

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
  (first (let [ciphered (oracle (repeat 1000 0xAA))]
           (filter (complement nil?)
                   (for [i (range 4 100)
                         :let [chunk    (take i ciphered)
                               ciphered (drop i ciphered)]]
                     (when (= (take i ciphered) chunk) i))))))

(defn make-dic
  "Make a dictionary of every possible (conj plain-bytes byte)"
  [cipher plain-bytes]
  {:pre [(vector? plain-bytes) (= 15 (mod (count plain-bytes) 16))]}
  (->> (range 256)
       (map (partial conj plain-bytes))
       (pmap #(vector (take (inc (count plain-bytes)) (cipher %)) (last %)))
       (into {})))

(def rand-byte (partial rand-int 256))
(defn random-bytes [n] (repeatedly n rand-byte))
(def random-key (partial random-bytes (* block-size word-size)))

(defn crack-one-byte-ecb [oracle known-secret-bytes block-size]
  (let [a-byte        0xAA
        padding-size  (- block-size (mod (count known-secret-bytes) block-size) 1)
        padding-bytes (repeat padding-size a-byte)
        crafted-block (take (+ padding-size (count known-secret-bytes) 1)
                            (oracle padding-bytes))
        dic           (make-dic oracle (vec (concat padding-bytes known-secret-bytes)))]
    (get dic crafted-block)))

(defn crack-ecb
  ([oracle]
   (when (ecb-mode? (oracle (repeat 1000 0xAA)))
     (crack-ecb oracle (detect-block-size oracle))))
  ([oracle block-size]
   (loop [acc []]
     (if-let [cracked-byte (crack-one-byte-ecb oracle acc block-size)]
       (do (println (char cracked-byte) cracked-byte)
           (recur (conj acc cracked-byte)))
       (drop-last acc)))))                                                      ;; drop last because it's 0x01 padding

