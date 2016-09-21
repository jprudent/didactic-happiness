(ns cryptopals.aes)

(def gf+ bit-xor)
(def gf- bit-xor)

(defn gf* [a b]
  (loop [a a b b p 0]
    (if (pos? b)
      (recur (gf+ (bit-shift-left a 1)
                  (if (pos? (bit-and 128 a)) 0x11B 0))
             (bit-shift-right b 1)
             (gf+ p (if (odd? b) a 0)))
      p)))


(def gf*inverse
  (memoize
    (fn [a]
      (if (zero? a)
        0
        (some #(when (= 1 (gf* a %)) %) (range 256))))))                        ; brute force

(defn byte-rotate-right [byte]
  (bit-or
    (bit-shift-right byte 1)
    (if (bit-test byte 0) 128 0)))

(defn bit-at [x n]
  (if (bit-test x n) 1 0))

(defn s-box [byte]
  (let [*-inverse (gf*inverse byte)
        c         0x63
        xor-at    (fn [bit n] (bit-xor bit (bit-at *-inverse (mod n 8))))
        sub-bit   (fn [i] (-> (xor-at 0 i)
                              (xor-at (+ i 4))
                              (xor-at (+ i 5))
                              (xor-at (+ i 6))
                              (xor-at (+ i 7))
                              (bit-xor (bit-at c i))))]
    (loop [i 0 result 0]
      (if (< i 8)
        (recur (inc i) (bit-or result (bit-shift-left (sub-bit i) i)))
        result))))

(def sub-word (partial map s-box))

(defn print-word [word]
  (print "|")
  (doseq [elem word]
    (print (format "%02X|" elem))))

(defn print-block [block]
  (doseq [word block]
    (do (print-word word)
        (println ""))))

(defn sub-bytes [state]
  (println "  - sub bytes")
  (println "    - IN : ")
  (print-block state)
  (map sub-word state))

(defn rot-word
  ([word] (rot-word word 1))
  ([word shift] (->> (cycle word)
                     (drop shift)
                     (take (count word)))))

(defn shift-rows [state]
  (println "  - shift rows")
  (println "    - IN : ")
  (print-block state)
  (map (partial rot-word)
       state (range)))

(defn mix-column [column]
  (let [ax (take (count column) (iterate #(rot-word % (dec (count %))) [0x02 0x03 0x01 0x01]))]
    (for [i (range (count column))]
      (reduce #(bit-xor %1 %2) 0 (map gf* (nth ax i) column)))))

(defn reverse-matrix [matrix]
  (apply map vector matrix))

(defn mix-columns [state]
  (println "  - mix columns")
  (println "    - IN : ")
  (print-block state)
  (reverse-matrix (map mix-column (reverse-matrix state))))

(defn xor-words [xs ys]
  (map bit-xor xs ys))

(defn add-round-key [state key]
  (println "  - Add round key")
  (println "    - IN : ")
  (print-block state)
  (println "    - KEY : ")
  (print-block key)
  (map xor-words state key))

;; key expansion

(def rcon                                                                       ;; rcon[i] = (nth rcon i)
  (map #(vector % 0 0 0) (concat [nil 1] (take 254 (iterate #(gf* 2 %) 2)))))   ;; rcon[0] is never used



(def key-size 4)
(def block-size 4)
(def nb-round 10)
(def word-size 4)

(defn next-word [expanded-key cpt-words]
  (let [temp             (last expanded-key)
        key-size-earlier (first (take-last key-size expanded-key))]
    (xor-words
      key-size-earlier
      (if (= 0 (mod cpt-words key-size))
        (-> (rot-word temp)                                                     ;; TODO implement AES 256 bits
            (sub-word)
            (xor-words (nth rcon (/ cpt-words key-size))))
        temp))))

(defn key-expansion
  "Expansion of the 128 bits key k.
  k is a coll of bytes for convenience
  result is a coll of words"
  [k]
  {:pre  [(= (* key-size word-size) (count k))]                                 ;; bytes
   :post [(= (* block-size (inc nb-round)) (count %))]}                         ;; words
  (loop [expanded-key (vec (partition word-size k))
         cpt-words    key-size]
    (if (< cpt-words (* block-size (inc nb-round)))
      (recur (conj expanded-key (next-word expanded-key cpt-words))
             (inc cpt-words))
      expanded-key)))

(defn cipher-block
  "cipher block against key
  block is a matrix of 4*Nb as described in section 3.4 of FIPS 197
  key is a sequence of bytes (that will be expanded in the implementation)
  returns the output in the same format as block"
  [block key]
  (let [expanded-key (key-expansion key)
        first-state  (add-round-key block
                                    (reverse-matrix (take block-size expanded-key)))]
    (loop [key   (drop block-size expanded-key)
           state first-state
           round 1]
      (println "--" "round" round "--")
      (print-block state)
      (if (< round nb-round)
        (recur (drop block-size key)
               (-> (sub-bytes state)
                   (shift-rows)
                   (mix-columns)
                   (add-round-key (reverse-matrix (take block-size key))))
               (inc round))
        (-> (sub-bytes state)
            (shift-rows)
            (add-round-key (reverse-matrix (take block-size key))))))))
