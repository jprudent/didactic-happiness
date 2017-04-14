(ns repicene.cpu
  (:require [repicene.history :as history]
            [repicene.decoder :refer [pc hex16 fetch decoder instruction-at-pc]]
            [repicene.instructions :refer [exec]]
            [repicene.schema :as s]))

(defn cpu-cycle [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? cpu)]}
  (try
    (history/save! cpu)
    (exec cpu (instruction-at-pc cpu))
    (catch Exception e
      (let [filename (str "coredump" (System/nanoTime))]
        (spit filename (prn-str (dissoc cpu :history-chan :debug-chan-rx :debug-chan-tx)))
        (println "exception occured at" (hex16 (pc cpu)) ". core dump in" filename)
        (.printStackTrace e)
        (throw e)))))
