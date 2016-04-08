(ns clj.core-test
  (:require [clojure.test :refer :all]
            [clj.core2 :refer :all]
            [clojure.test.check :as c]
            [clojure.test.check.generators :as g]
            [clojure.test.check.properties :as p]))

(def unsigned-byte (g/fmap #(mod % 256) g/pos-int))

(def test-xork
  (p/for-all [a unsigned-byte
              b unsigned-byte]
             (<= 0 (xork a b) 255)))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

