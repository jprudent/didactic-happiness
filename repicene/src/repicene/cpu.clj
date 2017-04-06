(ns repicene.cpu
  (:require [repicene.history :as history]
            [repicene.decoder :refer [fetch decoder]]
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
  (let [instr (instruction-at-pc cpu)
        #__     #_(println "before " (str "@" (hex16 (pc cpu))) ((:to-string instr) cpu))
        ret   (history/save cpu)
        ret   (exec ret instr)]
    ret
    ))
