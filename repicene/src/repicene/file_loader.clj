(ns repicene.file-loader
  (:require [clojure.java.io :as io])
  (:import (java.io DataInputStream EOFException)
           (java.util ArrayList)))

(defn load-rom
  "returns a vec of unsigned bytes representing the ROM file"
  [path]
  (vec (with-open [fis    (io/input-stream path)
                   reader (DataInputStream. fis)]
         (let [a (ArrayList.)]
           (try
             (loop [byte (.readUnsignedByte reader)]
               (do
                 (.add a byte)
                 (recur (.readUnsignedByte reader))))
             (catch EOFException _
               a))))))
