(ns repicene.cpu
  (:require [repicene.history :as history]
            [repicene.decoder :refer [exec pc hex16 fetch decoder instruction-at-pc]]
            [repicene.schema :as s]))

(defn cpu-cycle [cpu]
  {:pre  [(s/cpu? cpu)]
   :post [(s/cpu? cpu)]}
  (try
    (history/save! cpu)
    (exec (instruction-at-pc cpu) cpu)
    (catch Exception e
      (let [filename (str "coredump" (System/nanoTime))]
        (spit filename (prn-str (dissoc cpu :history-chan :debug-chan-rx :debug-chan-tx)))
        (println "exception occured at" (hex16 (pc cpu)) ". core dump in" filename)
        (.printStackTrace e)
        (throw e)))))
