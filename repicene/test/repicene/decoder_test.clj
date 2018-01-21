(ns repicene.decoder-test
  (:require [clojure.test :refer :all]
            [repicene.core :as core]
            [repicene.schema :as s]
            [repicene.debug :as debug]
            [repicene.decoder :as sut]
            [repicene.cpu-protocol :as cpu]
            [repicene.decoder :as decoder]))

(deftest permanent-breakpoint
  (let [cpu (-> (cpu/new-cpu (repeat 0x8000 0xDD))
                (debug/set-breakpoint 0 :permanent-breakpoint))
        cpu-breaked (sut/exec (sut/->Breakpoint) cpu)]
    (is (= ::s/break (:mode cpu-breaked)))
    (is (= 0xD3 (cpu/word-at cpu 0)))                                           ;; todo failing because of mutability, I hate this !
    (is (= 0xDD (cpu/word-at cpu-breaked 0)))))