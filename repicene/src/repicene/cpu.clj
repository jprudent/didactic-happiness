(ns repicene.cpu
  (:require [repicene.history :as history]
            [repicene.decoder :refer [exec pc hex16 fetch decoder instruction-at-pc]]
            [repicene.schema :as s]))

(defn cpu-cycle [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? cpu)]}
  (try
    #_(history/save! cpu)
    (let [instruction (instruction-at-pc cpu)]
      #_(spit "/tmp/log.repicene"
                (prn-str (into (::s/registers cpu) {:instr (dissoc instruction :to-string)}))
                :append true)
      (exec instruction cpu))
    (catch Exception e
      (let [filename (str "coredump" (System/nanoTime))]
        (spit filename (prn-str (dissoc cpu :history-chan :debug-chan-rx :debug-chan-tx)))
        (println "exception occured at" (hex16 (pc cpu)) ". core dump in" filename)
        (.printStackTrace e)
        (throw e)))))
