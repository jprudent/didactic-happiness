(ns clj.core
  (require [clj.hex-tools :refer [hex unsigned8 hex-simple]]
           [clj.data :refer :all]
           [clojure.math.combinatorics :as combo]))

(defrecord code-ref [data length magic])

(def p0 (->code-ref d08056100, 0x24, [0xf1 0xa1 0x04 0x08 0x01 0x01 0x00 0x00 0x9c 0x25 0x05 0x08]))
(def p1 (->code-ref d08050fa0, 0x2d80, [0x0f 0xbb 0x64 0x2d 0x7f 0x12 0x72 0xe4 0xc9 0x3d 0xf6 0x88]))
(def p2 (->code-ref d08049210, 0x7d5c, [0x35 0xc9 0x40 0xe2 0x97 0xde 0x83 0x53 0x3a 0xb3 0xdc 0x98]))
(def code-refs [p0 p1 p2])


(defn xork [k x]
  (mod (unsigned8 (- (bit-xor k x) 0xaa)) 256))

(defn bytes-at-pos [pos data]
  (for [i (range 0 (count data))
        :when (= pos (mod i 0xc))]
    (nth data i)))

(defn () combinations

  ([key-length]
   (apply combo/cartesian-product (repeat key-length (range 32 127))))

  ([key-length hint]
   (let [hint-len (count (first hint))]
     (map flatten
          (apply combo/cartesian-product
                 (conj (repeat (- key-length hint-len) (range 32 127))
                       hint))))))

(defn find-key-at-pos [^code-ref code-ref key-pos possible-keys-at-pos]
  (let [code-at-pos  (bytes-at-pos key-pos (:data code-ref))
        magic-number (get (:magic code-ref) key-pos)]

    (for [possible-key (map #(take (count code-at-pos) (flatten (repeat %))) possible-keys-at-pos)
          :let [xorks (map xork possible-key code-at-pos)
                _     (assert (= (count xorks) (count code-at-pos) (count possible-key)) (str (count xorks) "/" (count code-at-pos) "/" (count possible-key)))]
          :when (= magic-number (reduce bit-xor 0 xorks))]
      possible-key)))

(defn find-key

  ([^code-ref code-ref]
   (find-key code-ref (for [i (range 0 0xC)] [i (combinations (/ (:length code-ref) 0xC))])))

  ([^code-ref code-ref combinations-at-pos]
   (for [key-pos (range 0 0xC)]
     [key-pos (find-key-at-pos code-ref key-pos (second (nth combinations-at-pos key-pos)))])))



(defn compute-magic [key {:keys [data length]}]
  (for [key-pos (range 0 0xC)
        :let [expanded-key (take length (flatten (repeat key)))
              xored        (map xork data expanded-key)
              xored-0xC    (bytes-at-pos key-pos xored)]]
    xored-0xC))

(defn ok-key? [key {:keys [magic] :as code-ref}]
  (= magic (compute-magic key code-ref)))

(defn brute-force [code-refs key-len]
  {:pre [(<= 1 key-len 0x200)]}
  (for [key (combinations key-len)
        :when (every? (partial ok-key? key) code-refs)]
    key))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn magic-for-key-at-pos [xs-at-pos expanded-key]
  (let [xorks-at-pos   (map xork expanded-key xs-at-pos)
        computed-magic (reduce bit-xor 0 xorks-at-pos)]
    (assert (= (count xs-at-pos) (count expanded-key) (count xorks-at-pos)))
    [computed-magic expanded-key xs-at-pos xorks-at-pos]))

(defn expand-key [xs-at-pos key]
  (take (count xs-at-pos) (flatten (repeat key))))

(defn crack-p-at-pos [{:keys [data length magic] :as code-ref} key-pos]
  (let [xs-at-pos       (bytes-at-pos key-pos data)
        _               (println xs-at-pos)
        count-xs-at-pos (count xs-at-pos)
        keys            (map #(take count-xs-at-pos %) (combinations (min 43 count-xs-at-pos)))
        _               (println (first keys))
        magic-at-pos    (get magic key-pos)]
    (->> keys
         (pmap (partial magic-for-key-at-pos xs-at-pos))
         (filter #(= magic-at-pos (first %))))))


(defn crack-p-at-pos-with-hint [{:keys [data length magic] :as code-ref} key-pos keys-hint]
  (let [xs-at-pos       (bytes-at-pos key-pos data)
        _               (println xs-at-pos)
        count-xs-at-pos (count xs-at-pos)
        keys            (map #(expand-key xs-at-pos %) (combinations 5 keys-hint))
        _               (println (first keys))
        magic-at-pos    (get magic key-pos)]
    (->> keys
         (pmap (partial magic-for-key-at-pos xs-at-pos))
         (filter #(= magic-at-pos (first %))))))

