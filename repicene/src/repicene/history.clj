(ns repicene.history
  (:require [repicene.schema :as s]))

(defn save
  [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? cpu)]}
  (let [nohistory-cpu (assoc cpu ::s/history '())
        history   (-> (::s/history cpu)
                      (conj nohistory-cpu))]
    (->> (drop-last (- (count history) 100) history)
         (assoc cpu ::s/history))))

(defn restore
  [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(or (nil? %) (s/valid? %))]}
  (when-let [[previous & others] (::s/history cpu)]
    (assoc previous ::s/history others)))