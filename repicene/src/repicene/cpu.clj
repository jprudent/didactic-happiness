(ns repicene.cpu
  (:require [repicene.decoder :refer [exec hex16 fetch decoder instruction-at-pc]]
            [repicene.schema :as s]
            [repicene.cpu-protocol :as cpu]))

(defn prepare-core-dump [cpu]
  (-> (dissoc cpu :history-chan :debug-chan-rx :debug-chan-tx)
      (update :memory vec)))

(defn cpu-cycle [cpu]
  {:pre  [(s/cpu? cpu)]
   :post [(s/cpu? cpu)]}
  (try
    (-> (exec (instruction-at-pc cpu) cpu)
        (update :clock inc))
    (catch Exception e
      (let [filename (str "coredump" (System/nanoTime))]
        (spit filename (prn-str (prepare-core-dump cpu)))
        (println "exception occured at" (hex16 (cpu/get-pc cpu)) ". core dump in" filename)
        (.printStackTrace e)
        (throw e)))))
