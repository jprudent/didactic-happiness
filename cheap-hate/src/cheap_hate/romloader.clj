(ns cheap-hate.romloader
  (:import (java.io FileInputStream DataInputStream EOFException)
           (java.util ArrayList)))

;; I am so sorry about this code, you don't know man ...
(defn load-rom [path]
  (vec (with-open [fis    (FileInputStream. path)
                   reader (DataInputStream. fis)]
         (let [a (ArrayList.)]
           (try
             (loop [byte (.readUnsignedByte reader)]
               (do
                 (.add a byte)
                 (recur (.readUnsignedByte reader))))
             (catch EOFException _ a))))))
