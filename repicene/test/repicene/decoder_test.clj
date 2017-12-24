(ns repicene.decoder-test
  (:require [clojure.test :refer :all]
            [repicene.core :as core]
            [repicene.schema :as s]
            [repicene.debug :as debug]
            [repicene.decoder :as sut]))

(deftest permanent-breakpoint
  (let [cpu (-> (core/new-cpu (repeat 0x8000 0xDD))
                (debug/set-breakpoint 0 :permanent-breakpoint))
        cpu-breaked (sut/exec (sut/->Breakpoint) cpu)]
    (is (= ::s/break (::s/mode cpu-breaked)))
    (is (= 0xD3 (sut/word-at (::s/memory cpu) 0)))
    (is (= 0xDD (sut/word-at (::s/memory cpu-breaked) 0)))))
