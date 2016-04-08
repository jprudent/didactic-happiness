(ns clj.core2
  (require [clj.hex-tools :refer [hex unsigned8 hex-simple]]
           [clj.data :refer :all]
           [clojure.math.numeric-tower :refer [expt]])
  (:import (java.util.concurrent Executors)))

(defrecord code-ref [data length magic])

(def p0 (->code-ref d08056100, 0x24, [0xf1 0xa1 0x04 0x08 0x01 0x01 0x00 0x00 0x9c 0x25 0x05 0x08]))
(def p1 (->code-ref d08050fa0, 0x2d80, [0x0f 0xbb 0x64 0x2d 0x7f 0x12 0x72 0xe4 0xc9 0x3d 0xf6 0x88]))
(def p2 (->code-ref d08049210, 0x7d5c, [0x35 0xc9 0x40 0xe2 0x97 0xde 0x83 0x53 0x3a 0xb3 0xdc 0x98]))

(def ^:static KEY-LEN 4)
(def ^:static KEY-MIN 32)
(def ^:static KEY-MAX 127)
(def ^:static NB-THREADS 8)

(defn make-keygen [key-len min-val]
  (repeat key-len min-val))

(defn inc-keygen [[k0 & r] min-val max-val]
  (let [new-k0 (mod (inc k0) (inc max-val))]
    (if (zero? new-k0)
      (cons min-val (inc-keygen r min-val max-val))
      (cons new-k0 r))))

(defn xork [k x]
  {:pre  [(<= k 255) (<= x 255)]
   :post [(<= % 255)]}
  (unsigned8 (- (bit-xor k x) 0xaa)))

(defn bytes-at-pos [pos data]
  (for [i (range 0 (count data))
        :when (= pos (mod i 0xc))]
    (nth data i)))

(defn test-key [key data ^long magic-number]
  (let [expanded-key (take (count data) (flatten (repeat key)))
        xorks        (map xork expanded-key data)]
    (= magic-number (reduce bit-xor 0 xorks))))

(def keygen (atom (make-keygen KEY-LEN KEY-MIN)))
(def keymatch (atom []))

#_(defn crack [^code-ref {:keys [data magic] :as code-ref} keypos]
    (let [pool           (Executors/newFixedThreadPool NB-THREADS)
          data           (bytes-at-pos keypos data)
          test-a-key     (fn []
                           (let [key (swap! keygen inc-keygen KEY-MIN KEY-MAX)] ;; bug : first key is never tested
                             (when (test-key data key (get magic keypos))
                               (println "got a match : " key)
                               (swap! keymatch conj key))))
          number-of-keys (dec (expt (inc (- KEY-MAX KEY-MIN)) KEY-LEN))]
      (println "I will test" number-of-keys "keys")
      (dotimes [_ number-of-keys]
        (.execute pool test-a-key))
      (.shutdown pool)
      (while (not (.isTerminated pool)))
      (println "Bye")))

(defn crack-all [code-refs keypos]
  (let [pool             (Executors/newFixedThreadPool NB-THREADS)
        all-bytes-at-pos (map (comp (partial bytes-at-pos keypos) :data) code-refs)
        all-magics       (map (comp #(get % keypos) :magic) code-refs)
        test-a-key       (fn []
                           (let [key       (swap! keygen inc-keygen KEY-MIN KEY-MAX)
                                 tests-key (map (partial test-key key) all-bytes-at-pos all-magics)] ;; bug : first key is never tested
                             (when (every? true? tests-key)
                               (println "got a match : " key)
                               (swap! keymatch conj key))))
        number-of-keys   (dec (expt (inc (- KEY-MAX KEY-MIN)) KEY-LEN))]
    (println "I will test" number-of-keys "keys")
    (dotimes [_ number-of-keys]
      (.execute pool test-a-key))
    (.shutdown pool)
    (while (not (.isTerminated pool)))
    (println "Bye")))
