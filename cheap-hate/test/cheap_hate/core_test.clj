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
    (testing "Bnnn - JP V0, nnn - should jump to address nnn + V0."
      (let [program  [0x60 0x04                             ;; 0x200: mov V0, 0x04
                      0xB2 0x04                             ;; 0x202: jp 0x204
                      0x00 0x00                             ;; 0x204: halt (skipped)
                      0x60 0x11                             ;; 0x206: mov V0, 0x11  (skipped)
                      0x00 0x00]                            ;; 0x208: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0 0x04)
                         (assoc :PC 0x208))
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

  (testing "Setting registers"
    (testing "I can be set to address"
      (let [program  [0xA0 0x42                             ;; 0x200: mov I, 0x42
                      0x00 0x00]                            ;; 0x202: halt
            expected (-> (load-program fresh-machine program)
                         (assoc :I 0x42)
                         inc-pc)
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "VX can be set to VY"
      (let [program  [0x66 0x42                             ;; 0x200: mov V6, 0x42
                      0x67 0xBF                             ;; 0x202: mov V7, 0xBF
                      0x86 0x70                             ;; 0x204: mov V6, V7
                      0x00 0x00]                            ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x6 0xBF 0x7 0xBF)
                         (assoc :PC 0x206))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "RND Vx, nn should Set Vx = random byte AND kk."
      (let [program  [0x66 0x42                             ;; 0x200: mov V6, 0x42
                      0xC6 0xAA                             ;; 0x202: rnd V6, 0xAA
                      0x00 0x00]                            ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x6 0x28)
                         (assoc :prn 45)
                         (assoc :PC 0x204))
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
    (testing "When adding a value, it should overflow without affecting carry register"
      (let [program  [0x61 0x42                             ;; 0x200: mov V1, 0x42
                      0x71 0xBF                             ;; 0x202: add V1, 0xBE = 0x101
                      0x00 0x00]                            ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x1)
                         (assoc :PC 0x204))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "When adding a register, it should overflow affecting carry register"
      (let [program  [0x61 0x42                             ;; 0x200: mov V1, 0x42
                      0x62 0xBF                             ;; 0x202: mov V2, 0xBF
                      0x81 0x24                             ;; 0x204: add V1, V2 = 0x101
                      0x00 0x00]                            ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x1, 0x2 0xBF, 0xF 0x1)
                         (assoc :PC 0x206))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "When substracting a register, it should overflow affecting carry register to 0 if overflow"
      (let [program  [0x61 0x42                             ;; 0x200: mov V1, 0x42
                      0x62 0xBF                             ;; 0x202: mov V2, 0xBF
                      0x81 0x25                             ;; 0x204: sub V1, V2 = 0x101
                      0x00 0x00]                            ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x83, 0x2 0xBF, 0xF 0x0)
                         (assoc :PC 0x206))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "When substracting a register, it should overflow affecting carry register to 1 if no overflow"
      (let [program  [0x61 0xBF                             ;; 0x200: mov V1, 0xBF
                      0x62 0x42                             ;; 0x202: mov V2, 0x42
                      0x81 0x25                             ;; 0x204: sub V1, V2 = 0x101
                      0x00 0x00]                            ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x7D 0x2 0x42, 0xF 0x1)
                         (assoc :PC 0x206))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "SUBN should Set Vx = Vy - Vx, set VF = NOT borrow"
      (let [program  [0x61 0x42                             ;; 0x200: mov V1, 0x42
                      0x62 0xBF                             ;; 0x202: mov V2, 0xBF
                      0x81 0x27                             ;; 0x204: sub V1, V2 = 0x101
                      0x00 0x00]                            ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x7D 0x2 0xBF, 0xF 0x1)
                         (assoc :PC 0x206))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "register can be ORed against another register"
      (let [program  [0x61 0x0F                             ;; 0x200: mov V1, 0x0F
                      0x62 0xF0                             ;; 0x202: mov V2, 0xF0
                      0x81 0x21                             ;; 0x204: or V1, V2 = 0x101
                      0x00 0x00]                            ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0xFF, 0x2 0xF0)
                         (assoc :PC 0x206))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "register can be ANDed against another register"
      (let [program  [0x61 0x0F                             ;; 0x200: mov V1, 0x0F
                      0x62 0xF0                             ;; 0x202: mov V2, 0xF0
                      0x81 0x22                             ;; 0x204: and V1, V2 = 0x101
                      0x00 0x00]                            ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x00, 0x2 0xF0)
                         (assoc :PC 0x206))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "register can be XORed against another register"
      (let [program  [0x61 0xAB                             ;; 0x200: mov V1, 0xAB
                      0x62 0x55                             ;; 0x202: mov V2, 0x55
                      0x81 0x23                             ;; 0x204: xor V1, V2
                      0x00 0x00]                            ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0xFE, 0x2 0x55)
                         (assoc :PC 0x206))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "shift right a register, overflowing in VF"
      (let [program  [0x61 0x03                             ;; 0x200: mov V1, 0x03
                      0x81 0xF6                             ;; 0x202: shr V1
                      0x00 0x00]                            ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x01, 0xF 0x01)
                         (assoc :PC 0x204))
            actual   (start-machine program)]
        (is (= actual expected))))
    (testing "shift left a register, overflowing in VF"
      (let [program  [0x61 0xFF                             ;; 0x200: mov V1, 0xFF
                      0x81 0x0E                             ;; 0x202: shl V1
                      0x00 0x00]                            ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0xFE, 0xF 0x01)
                         (assoc :PC 0x204))
            actual   (start-machine program)]
        (is (= actual expected))))))
