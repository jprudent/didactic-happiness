(ns core-async-monkey.core-test
  (:require [clojure.test :refer :all]
            [core-async-monkey.core :refer :all]
            [clojure.core.async :refer [chan go-loop >! <! pipeline]])
  (:import (java.util ArrayList)))

(deftest test-chan
  (testing "Chan with supplied transducer should filter even numbers"
    (is (let [in (chan 1 (filter even?))]
          (go-loop [[x & others] (range 10)]
            (when x
              (>! in x)
              (recur others)))
          (go-loop [prev -1]
            (let [even-x (<! in)]
              (println even-x)
              (is (> even-x prev) "Result is ordered")
              (is (even? even-x) "Filter is applied")
              (recur even-x)))))))

(deftest test-pipeline
  (testing "Pipeline should filter even numbers"
    (let [out  (chan)
          in   (chan)
          _ (pipeline 5 out (filter even?) in true)]
      (go-loop [[x & others] (range 40000)]
        (when x
          (>! in x)
          (recur others)))
      (go-loop [prev -1]
        (let [even-x (<! out)]
          (println even-x)
          (is (> even-x prev) "Result is ordered")
          (is (even? even-x) "Filter is applied")
          (recur even-x))))))


(defn get-page [idx callback]
  (println "Fetching page " idx)
  (Thread/sleep 100)
  (future
    (callback (vec (range (* idx 5)
                   (* (inc idx) 5))))))

(defn pages [xf rf finisher-callback]
  (let [f (xf rf)]))

(def xform (comp cat
                 (filter #(> % 10))
                 (take 10)))

(pages xform
       conj
       (fn [result]
         (println "Finished with " result)))




