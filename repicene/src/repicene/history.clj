(ns repicene.history
  (:require [repicene.schema :as s]
            [clojure.core.async :refer [go chan sliding-buffer <!! >!! >! go-loop alts!! timeout]]))

(defn save!
  [{:keys [history-chan] :as cpu}]
  {:pre  [(s/cpu? cpu)]
   :post [(s/cpu? cpu)]}
  (>!! history-chan cpu))

(defn read-chan [chan]
  (first (alts!! [chan (timeout 10)])))

(defn restore!
  [{:keys [history-chan] :as cpu}]
  {:pre  [(s/cpu? cpu)]
   :post [(or (nil? %) (s/cpu? %))]}
  (loop [older  (read-chan history-chan)
         backup '()]
    (if older
      (let [value (read-chan history-chan)]
        (recur value (conj backup older)))
      (let [[most-recent & elderly] backup]
        (loop [[recent & others] elderly]
          (when recent
            (>!! history-chan recent)
            (recur others)))
        (or most-recent cpu)))))