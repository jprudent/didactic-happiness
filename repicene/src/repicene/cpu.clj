(ns repicene.cpu
  (:require [repicene.history :as history]
            [repicene.decoder :refer [pc hex16 fetch decoder]]
            [repicene.instructions :refer [exec]]
            [repicene.schema :as s]))

(defn instruction-at-pc [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(not (nil? %))]}
  (get decoder (fetch cpu)))

(defn start-debugging [cpu]
  (assoc cpu :debugging? true))

(defn stop-debugging [cpu]
  (assoc cpu :debugging? false))

(defn cpu-cycle [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? cpu)]}
  (try
    (let [instr (instruction-at-pc cpu)
          ret   (history/save cpu)
          ret   (exec ret instr)]
      ret)
    (catch Exception e
      (let [filename (str "coredump" (System/nanoTime))]
        (spit filename (prn-str (dissoc cpu ::s/history :debug-chan-rx :debug-chan-tx)))
        (println "exception occured at" (hex16 (pc cpu)) ". core dump in" filename)
        (throw e)))))
