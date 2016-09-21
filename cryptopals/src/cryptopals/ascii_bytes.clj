(ns cryptopals.ascii-bytes
  "This namespaces is about bytes written as ascii")

(def ^:static hexchar->byte
  {\0 0x0, \1 0x1, \2 0x2, \3 0x3, \4 0x4, \5 0x5, \6 0x6, \7 0x7, \8 0x8, \9 0X9,
   \A 0xA, \B 0xB, \C 0xC, \D 0xD, \E 0xE, \F 0xF
   \a 0xA, \b 0xB, \c 0xC, \d 0xD, \e 0xE, \f 0xF})

(defn hexstring->bytes
  "Take an hexadecimal string and returns a seq of bytes"
  [s]
  (map (fn [[b1 b0]]
         (bit-or (bit-shift-left (hexchar->byte b1) 4)
                 (hexchar->byte b0)))
       (partition 2 s)))

(def ^:static base64-index-table
  {0  \A, 16 \Q, 32 \g, 48 \w
   1  \B, 17 \R, 33 \h, 49 \x
   2  \C, 18 \S, 34 \i, 50 \y
   3  \D, 19 \T, 35 \j, 51 \z
   4  \E, 20 \U, 36 \k, 52 \0
   5  \F, 21 \V, 37 \l, 53 \1
   6  \G, 22 \W, 38 \m, 54 \2
   7  \H, 23 \X, 39 \n, 55 \3
   8  \I, 24 \Y, 40 \o, 56 \4
   9  \J, 25 \Z, 41 \p, 57 \5
   10 \K, 26 \a, 42 \q, 58 \6
   11 \L, 27 \b, 43 \r, 59 \7
   12 \M, 28 \c, 44 \s, 60 \8
   13 \N, 29 \d, 45 \t, 61 \9
   14 \O, 30 \e, 46 \u, 62 \+
   15 \P, 31 \f, 47 \v, 63 \/})

(defn replace-padding [string]
  (cond
    (clojure.string/ends-with? string "AA")
    (str (subs string 0 (- (count string) 2)) "==")
    (clojure.string/ends-with? string "A")
    (str (subs string 0 (dec (count string))) "=")
    :else string))

(defn bytes->base64
  "Encode bytes to a base64 string"
  [bytes]
  (->> (map
         (fn [[b0 b1 b2]]
           [(bit-shift-right (bit-and 2r11111100 b0) 2)
            (bit-or (bit-shift-left (bit-and 2r00000011 b0) 4)
                    (bit-shift-right (bit-and 2r11110000 b1) 4))
            (bit-or (bit-shift-left (bit-and 2r00001111 b1) 2)
                    (bit-shift-right (bit-and 2r11000000 b2) 6))
            (bit-and 2r00111111 b2)])
         (partition 3 3 [0 0] bytes))
       (mapcat #(map base64-index-table %))
       (apply str)
       (replace-padding)))

(defn bytes->ascii-string
  "Convert bytes to ASCII string"
  [bytes]
  (apply str (map char bytes)))

(defn bytes->hexstring
  "Convert bytes to hex string"
  [bytes]
  (apply str (map #(format "%02x" %) bytes)))

(defn ascii-string->bytes
  "Convert an ASCII string to bytes"
  [s]
  (map byte s))


(def ^:static base64-reversed-index-map
  (clojure.set/map-invert base64-index-table))

(defn base64-chunk->bytes [[a b c d]]
  (filter (comp not nil?)
          [(bit-or (bit-shift-left a 2) (bit-shift-right (bit-and 2r110000 b) 4))
           (when c
             (bit-or (bit-shift-left (bit-and 2r001111 b) 4) (bit-shift-right (bit-and 2r111100 c) 2)))
           (when d
             (bit-or (bit-shift-left (bit-and 2r000011 c) 6) d))]))

(defn base64->bytes
  "Take a base 64 ascii string and returns a coll of bytes"
  [b64]
  (mapcat base64-chunk->bytes
          (partition 4 (map base64-reversed-index-map b64))))