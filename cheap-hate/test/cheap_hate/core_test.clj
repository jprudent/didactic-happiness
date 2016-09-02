(ns cheap-hate.core-test
  (:require [clojure.test :refer :all]
            [cheap-hate.core :refer :all]))


(testing "Call stack"
  (let [program [0x22 0x04                                  ;; 0x200: call @0x204
                 0x00 0x00                                  ;; 0x202: halt
                 0x00 0xEE                                  ;; 0x204: ret
                 ]
        m0      (load-program fresh-machine program)
        m1      (start-machine program)]
    (is (= m0 m1))))
