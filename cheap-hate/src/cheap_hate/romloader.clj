(ns cheap-hate.romloader
  (:require [clojure.java.io :as io])
  (:import (java.io DataInputStream EOFException)
           (java.util ArrayList)))

;; I am so sorry about this code, you don't know man ...
(defn load-rom [path]
  (vec (with-open [fis    (io/input-stream path)
                   reader (DataInputStream. fis)]
         (let [a (ArrayList.)]
           (try
             (loop [byte (.readUnsignedByte reader)]
               (do
                 (.add a byte)
                 (recur (.readUnsignedByte reader))))
             (catch EOFException _ a))))))
