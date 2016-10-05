(ns cryptopals.aes)

;; some cool debugging utils ^^

(defn print-word [word]
  (print "|")
  (doseq [elem word]
    (print (format "%02X|" elem))))

(defn print-block [block]
  (doseq [word block]
    (do (print-word word)
        (println ""))))


;; Some Galois field arithmetic

(def gf+ bit-xor)

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


;; bits, bytes and words utilities

(defn cycle-bits-right [byte]
  (bit-or
    (bit-shift-right byte 1)
    (if (bit-test byte 0) 128 0)))

(defn bit-at [x n]
  (if (bit-test x n) 1 0))

(defn rot-word
  ([word] (rot-word word 1))
  ([word shift] (->> (cycle word)
                     (drop shift)
                     (take (count word)))))

(defn reverse-matrix [matrix]
  (apply map vector matrix))

;; SubBytes and InvSubBytes operations

(defn affine-transformation
  [byte mask c]
  (let [xor-at  (fn [bit n] (bit-xor bit (bit-at byte (mod n 8))))
        sub-bit (fn [i] (-> (reduce #(xor-at %1 (+ i %2)) 0 mask)
                            (bit-xor (bit-at c i))))]
    (reduce #(bit-or %1 (bit-shift-left (sub-bit %2) %2))
            0 (range 8))))

(def s-box
  (memoize (fn [byte]
             (affine-transformation (gf*inverse byte) [0 4 5 6 7] 0x63))))

(def inv-s-box
  (memoize (fn [byte]
             (gf*inverse (affine-transformation byte [2 5 7] 2r101)))))

(defn -sub-bytes [state box]
  (map #(map box %) state))

(defn sub-bytes [state]
  (-sub-bytes state s-box))

(defn inv-sub-bytes [state]
  (-sub-bytes state inv-s-box))


;; ShiftRows and InvShiftRows operations

(defn shift-rows [state]
  (map (partial rot-word)
       state (range)))

(defn inv-shift-row [state]
  (map (fn [row shift] (rot-word row (- (count row) shift)))
       state (range)))

;; MixColumns and InvMixColumns operations

(defn mix-column [column polynomial]
  (let [ax (take (count column) (iterate #(rot-word % (dec (count %))) polynomial))]
    (for [i (range (count column))]
      (reduce #(bit-xor %1 %2) 0 (map gf* (nth ax i) column)))))

(defn mix-columns [state]
  (reverse-matrix (map #(mix-column % [0x02 0x03 0x01 0x01]) (reverse-matrix state))))

(defn inv-mix-columns [state]
  (reverse-matrix (map #(mix-column % [0x0E 0x0B 0x0D 0x09]) (reverse-matrix state))))

;; AddRoundKey operation

(defn xor-words [x y]
  (map bit-xor x y))

(defn xor-blocks [state key]
  (map xor-words state key))

(def add-round-key xor-blocks)

;; Key expansion

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
        (->> (rot-word temp)                                                    ;; TODO implement AES 256 bits
             (map s-box)
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

;; Cipher and Decipher a block

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

(defn decipher-block
  "cipher block against key
  block is a matrix of 4*Nb as described in section 3.4 of FIPS 197
  key is a sequence of bytes (that will be expanded in the implementation)
  returns the output in the same format as block"
  [block key]
  (let [expanded-key (partition block-size (key-expansion key))
        first-state  (add-round-key block
                                    (reverse-matrix (last expanded-key)))]
    (loop [key   (drop-last expanded-key)
           state first-state
           round 1]
      (if (< round nb-round)
        (recur (drop-last key)
               (-> (inv-shift-row state)
                   (inv-sub-bytes)
                   (add-round-key (reverse-matrix (last key)))
                   (inv-mix-columns))
               (inc round))
        (-> (inv-shift-row state)
            (inv-sub-bytes)
            (add-round-key (reverse-matrix (last key))))))))

(defn blocks->bytes
  "Convert a seq of blockes as described in section 3.4 of FIPS 197
  in a sequence of bytes"
  [blocks]
  (flatten (map #(flatten (reverse-matrix %1)) blocks)))

(defn bytes->blocks
  "returns a seq of blocks as described in section 3.4 of FIPS 197"
  [bytes]
  (map #(reverse-matrix (partition block-size %1))
       (partition (* word-size block-size) bytes)))

(defn cipher-ecb
  [plain-bytes key]
  {:pre [(= 0 (mod (count plain-bytes) (* block-size word-size)))]}             ;; padding is not supported
  (map #(cipher-block %1 key) (bytes->blocks plain-bytes)))

(defn decipher-ecb
  [ciphered-bytes key]
  {:pre [(= 0 (mod (count ciphered-bytes) (* block-size word-size)))]}          ;; padding is not supported
  (map #(decipher-block %1 key) (bytes->blocks ciphered-bytes)))

(defn pkcs7-padding
  [bytes block-size]
  {:pre  [(pos? (count bytes))]
   :post [(zero? (mod (count %) block-size))]}
  (let [padding-size (- block-size (mod (count bytes) block-size))]
    (concat bytes (repeat padding-size padding-size))))

(defn cipher-cbc
  [plain-bytes key iv]
  {:pre [(= 0 (mod (count plain-bytes) (* block-size word-size)))]}             ;; padding is not supported
  (let [iv-block (first (bytes->blocks iv))]
    (reduce (fn [result block]
              (conj result
                    (cipher-block (xor-blocks block (or (last result) iv-block))
                                  key)))
            []
            (bytes->blocks plain-bytes))))

(defn decipher-cbc
  [ciphered-bytes key iv]
  {:pre [(= 0 (mod (count ciphered-bytes) (* block-size word-size)))]}          ;; padding is not supported
  (let [ciphered-blocks (bytes->blocks ciphered-bytes)
        iv-block        (first (bytes->blocks (pkcs7-padding iv (* block-size word-size))))]
    (reduce (fn [result [previous-block block]]
              (conj result
                    (xor-blocks (decipher-block block key)
                                previous-block)))
            []
            (map vector
                 (concat [iv-block] ciphered-blocks)
                 ciphered-blocks))))


