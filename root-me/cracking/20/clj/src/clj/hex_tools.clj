(ns clj.hex-tools)

(defn hex [n] (format "0x%02x" n))

(defn hex-simple [n] (format "%02x" n))

(defn unsigned8 [x]
  (if (neg? x) (+ x 256) x))