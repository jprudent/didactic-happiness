(ns cheap-hate.core-test
  (:require [clojure.test :refer :all]
            [cheap-hate.core :refer :all]))


(deftest cheap-hate-test
  (testing "Call stack"
    (testing "Calling and returning"
      (let [program  [0x22 0x04                             ;; 0x200: call @0x204
                      0x00 0x00                             ;; 0x202: halt
                      0x00 0xEE]                            ;; 0x204: ret

            expected (-> (load-program fresh-machine program) inc-pc)
            actual   (start-machine program)]
        (is (= actual expected)))))

  (testing "Jumps"
    (testing "Imperative jump"
      (let [program  [0x12 0x04                             ;; 0x200: jmp @0x204
                      0x00 0x00                             ;; 0x202: halt
                      0x12 0x02]                            ;; 0x204: jmp @0x202

            expected (-> (load-program fresh-machine program) inc-pc)
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "Conditional skip if register equals value"
      (let [program  [0x60 0xAA                             ;; 0x200: mov V0, 0xAA
                      0x30 0xAA                             ;; 0x202: ske V0, 0xAA (skip)
                      0x00 0x00                             ;; 0x204: halt (never reached)
                      0x30 0x42                             ;; 0x206: ske V0, 0x42
                      0x60 0xAB                             ;; 0x208: mov V0, 0xAB (reached)
                      0x00 0x00                             ;; 0x20A: halt
                      ]
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0 0xAB)
                         (assoc :PC 0x20A))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "Conditional skip if register not equals value"
      (let [program  [0x60 0xAA                             ;; 0x200: mov V0, 0xAA
                      0x40 0xAB                             ;; 0x202: skne V0, 0xAB (skip)
                      0x00 0x00                             ;; 0x204: halt (never reached)
                      0x40 0xAA                             ;; 0x206: skne V0, 0xAA
                      0x60 0xAB                             ;; 0x208: mov V0, 0xAB (reached)
                      0x00 0x00                             ;; 0x20A: halt
                      ]
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0 0xAB)
                         (assoc :PC 0x20A))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "Conditional skip if register equals another register"
      (let [program  [0x60 0xAA                             ;; 0x200: mov V0, 0xAA
                      0x6E 0xAA                             ;; 0x202: mov VE, 0xAA
                      0x6D 0xDD                             ;; 0x204: mov VD, 0xDD
                      0x50 0xE0                             ;; 0x206: ske V0, VE (skip)
                      0x00 0x00                             ;; 0x208: halt (never reached)
                      0x50 0xD0                             ;; 0x20A: ske V0, VD
                      0x60 0xAB                             ;; 0x20C: mov V0, 0xAB (reached)
                      0x00 0x00                             ;; 0x20E: halt
                      ]
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0 0xAB 0xE 0xAA 0xD 0xDD)
                         (assoc :PC 0x20E))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "Conditional skip if register not equals another register"
      (let [program  [0x60 0xAA                             ;; 0x200: mov V0, 0xAA
                      0x6E 0xAA                             ;; 0x202: mov VE, 0xAA
                      0x6D 0xDD                             ;; 0x204: mov VD, 0xDD
                      0x9D 0x00                             ;; 0x206: skne VD, V0 (skip)
                      0x00 0x00                             ;; 0x208: halt (never reached)
                      0x90 0xE0                             ;; 0x20A: skne V0, VE
                      0x60 0xAB                             ;; 0x20C: mov V0, 0xAB (reached)
                      0x00 0x00                             ;; 0x20E: halt
                      ]
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0 0xAB 0xE 0xAA 0xD 0xDD)
                         (assoc :PC 0x20E))
            actual   (start-machine program)]
        (is (= actual expected)))))

  (testing "I register"
    (testing "It can be set to address"
      (let [program  [0xA0 0x42                             ;; 0x200: mov I, 0x42
                      0x00 0x00]                            ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (assoc :I 0x42)
                         inc-pc)
            actual   (start-machine program)]
        (is (= actual expected)))))
  (testing "Arithmetic"
    (testing "It should add a number to a register"
      (let [program  [0x61 0x42                             ;; 0x200: mov V1, 0x42
                      0x71 0x11                             ;; 0x202: add V1, 0x11
                      0x00 0x00]                            ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 1 0x53)
                         (assoc :PC 0x204))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "It should overflow without affecting carry register"
          (let [program  [0x61 0x42                             ;; 0x200: mov V1, 0x42
                          0x71 0xBF                             ;; 0x202: add V1, 0xBE = 0x101
                          0x00 0x00]                            ;; 0x204: halt
                expected (-> (load-program fresh-machine program)
                             (update :registers assoc 1 0x01)
                             (assoc :PC 0x204))
                actual   (start-machine program)]
            (is (= actual expected))))))
