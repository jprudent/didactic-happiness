(ns repicene.history
  (:require [repicene.schema :as s]))

(defn save
  [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? cpu)]}
  (let [history (-> (::s/history cpu)
                    (conj cpu))]
    (->> (drop-last (- (count history) 100) history)
         (assoc cpu ::s/history))))

(defn restore
  [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(or (nil? %) (s/valid? %))]}
  (first (::s/history cpu)))