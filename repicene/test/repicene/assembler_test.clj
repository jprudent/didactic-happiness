(ns repicene.assembler-test
  (:require [clojure.test :refer :all])
  (:require [repicene.assembler :refer [assemble]]))

(deftest assemble-test
  (is (= [0x00] (assemble "nop")))

  (is (= [0x01 0x01 0x00] (assemble "ld bc 1")))
  (is (= [0x01 0x00 0x00] (assemble "ld bc 0")))
  (is (= [0x01 0xFF 0x00] (assemble "ld bc 255")))
  (is (= [0x01 0x00 0x01] (assemble "ld bc 256")))

  (is (= [0x02] (assemble "ld <bc> a")))

  (is (= [0x03] (assemble "inc bc")))

  (is (= [0xC9] (assemble "ret")))
  (is (= [0xCD 0X00 0x01] (assemble "call 256"))))
