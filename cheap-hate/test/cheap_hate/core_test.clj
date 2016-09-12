(ns cheap-hate.core-test
  (:require [clojure.test :refer :all]
            [cheap-hate.core :refer :all]
            [cheap-hate.romloader :refer :all]
            [cheap-hate.instructions :refer :all]
            [cheap-hate.simple-machine :refer :all]))

(defrecord MuteScreen []
  Screen
  (print-screen [this _ _] this))

(defrecord AtomKeyboard [a-val]
  Keyboard
  (read-device [this] this)
  (pressed-key [_] @a-val))

(defrecord MuteFlightRecorder []
  FlightRecorder
  (record [_ _ _]))

(defrecord TheFastestClock []
  Clock
  (throttle [this] this))

(def current-key (atom nil))

(defn str->bits [s]
  (map (fn [bit-as-string] (Integer/parseInt (str bit-as-string))) s))

(defn to-bit-array [a]
  (vec (map (comp vec str->bits) a)))

(defn launch [program] (start-machine
                         fresh-machine
                         {:screen          (->MuteScreen)
                          :flight-recorder (->MuteFlightRecorder)
                          :keyboard        (->AtomKeyboard current-key)
                          :program         program
                          :clock           (->TheFastestClock)}))

(deftest simple-machine-test
  (testing "Call stack"

    (testing "Calling and returning"
      (let [program  [0x22 0x04                                                 ;; 0x200: call @0x204
                      0x00 0x00                                                 ;; 0x202: halt
                      0x00 0xEE]                                                ;; 0x204: ret

            expected (-> (load-program fresh-machine program) inc-pc)
            actual   (launch program)]
        (is (= actual expected)))))

  (testing "Jumps"
    (testing "Imperative jump"
      (let [program  [0x12 0x04                                                 ;; 0x200: jmp @0x204
                      0x00 0x00                                                 ;; 0x202: halt
                      0x12 0x02]                                                ;; 0x204: jmp @0x202

            expected (-> (load-program fresh-machine program) inc-pc)
            actual   (launch program)]
        (is (= actual expected))))
    (testing "Bnnn - JP V0, nnn - should jump to address nnn + V0."
      (let [program  [0x60 0x04                                                 ;; 0x200: mov V0, 0x04
                      0xB2 0x04                                                 ;; 0x202: jp 0x204
                      0x00 0x00                                                 ;; 0x204: halt (skipped)
                      0x60 0x11                                                 ;; 0x206: mov V0, 0x11  (skipped)
                      0x00 0x00]                                                ;; 0x208: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0 0x04)
                         (assoc :PC 0x208))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "Conditional skip if register equals value"
      (let [program  [0x60 0xAA                                                 ;; 0x200: mov V0, 0xAA
                      0x30 0xAA                                                 ;; 0x202: ske V0, 0xAA (skip)
                      0x00 0x00                                                 ;; 0x204: halt (never reached)
                      0x30 0x42                                                 ;; 0x206: ske V0, 0x42
                      0x60 0xAB                                                 ;; 0x208: mov V0, 0xAB (reached)
                      0x00 0x00                                                 ;; 0x20A: halt
                      ]
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0 0xAB)
                         (assoc :PC 0x20A))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "Conditional skip if register not equals value"
      (let [program  [0x60 0xAA                                                 ;; 0x200: mov V0, 0xAA
                      0x40 0xAB                                                 ;; 0x202: skne V0, 0xAB (skip)
                      0x00 0x00                                                 ;; 0x204: halt (never reached)
                      0x40 0xAA                                                 ;; 0x206: skne V0, 0xAA
                      0x60 0xAB                                                 ;; 0x208: mov V0, 0xAB (reached)
                      0x00 0x00                                                 ;; 0x20A: halt
                      ]
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0 0xAB)
                         (assoc :PC 0x20A))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "Conditional skip if register equals another register"
      (let [program  [0x60 0xAA                                                 ;; 0x200: mov V0, 0xAA
                      0x6E 0xAA                                                 ;; 0x202: mov VE, 0xAA
                      0x6D 0xDD                                                 ;; 0x204: mov VD, 0xDD
                      0x50 0xE0                                                 ;; 0x206: ske V0, VE (skip)
                      0x00 0x00                                                 ;; 0x208: halt (never reached)
                      0x50 0xD0                                                 ;; 0x20A: ske V0, VD
                      0x60 0xAB                                                 ;; 0x20C: mov V0, 0xAB (reached)
                      0x00 0x00                                                 ;; 0x20E: halt
                      ]
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0 0xAB 0xE 0xAA 0xD 0xDD)
                         (assoc :PC 0x20E))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "Conditional skip if register not equals another register"
      (let [program  [0x60 0xAA                                                 ;; 0x200: mov V0, 0xAA
                      0x6E 0xAA                                                 ;; 0x202: mov VE, 0xAA
                      0x6D 0xDD                                                 ;; 0x204: mov VD, 0xDD
                      0x9D 0x00                                                 ;; 0x206: skne VD, V0 (skip)
                      0x00 0x00                                                 ;; 0x208: halt (never reached)
                      0x90 0xE0                                                 ;; 0x20A: skne V0, VE
                      0x60 0xAB                                                 ;; 0x20C: mov V0, 0xAB (reached)
                      0x00 0x00                                                 ;; 0x20E: halt
                      ]
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0 0xAB 0xE 0xAA 0xD 0xDD)
                         (assoc :PC 0x20E))
            actual   (launch program)]
        (is (= actual expected)))))

  (testing "Setting registers"
    (testing "I can be set to address"
      (let [program  [0xA0 0x42                                                 ;; 0x200: mov I, 0x42
                      0x00 0x00]                                                ;; 0x202: halt
            expected (-> (load-program fresh-machine program)
                         (assoc :I 0x42)
                         inc-pc)
            actual   (launch program)]
        (is (= actual expected))))
    (testing "I can added to a register without handling carry (I is a 16 bits register)"
      (let [program  [0x6B 0xFF                                                 ;; 0x200: mov VB, 0xFF
                      0xA1 0xFF                                                 ;; 0x202: mov I, 0xFF
                      0xFB 0x1E                                                 ;; 0x204: add I, VB
                      0x00 0x00]                                                ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0xB 0xFF)
                         (assoc :I 0x2FE)
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "I can be set to font location"
      (let [program  [0x61 0x0A                                                 ;; 0x200: mov V1, 0xA
                      0xF1 0x29                                                 ;; 0x202: ldf I, V1 (load the font for 'A' which is pointed by V1)
                      0x00 0x00]                                                ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0xA)
                         (assoc :I 0x32)
                         (assoc :PC 0x204))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "VX can be set to VY"
      (let [program  [0x66 0x42                                                 ;; 0x200: mov V6, 0x42
                      0x67 0xBF                                                 ;; 0x202: mov V7, 0xBF
                      0x86 0x70                                                 ;; 0x204: mov V6, V7
                      0x00 0x00]                                                ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x6 0xBF 0x7 0xBF)
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "RND Vx, nn should Set Vx = random byte AND kk."
      (let [program  [0x66 0x42                                                 ;; 0x200: mov V6, 0x42
                      0xC6 0xAA                                                 ;; 0x202: rnd V6, 0xAA
                      0x00 0x00]                                                ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x6 0x28)
                         (assoc :prn 45)
                         (assoc :PC 0x204))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "VX is waiting to be set to key pressed"
      (let [program  [0xF8 0x0A                                                 ;; 0x200: mov V8, K
                      0x00 0x00]                                                ;; 0x202: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x8 0xE)
                         (assoc :PC 0x202)
                         (assoc :keyboard 0xE))
            actual   (future (launch program))]
        (reset! current-key 0xE)
        (is (= @actual expected))
        (reset! current-key nil))))
  (testing "Arithmetic"
    (testing "It should add a number to a register"
      (let [program  [0x61 0x42                                                 ;; 0x200: mov V1, 0x42
                      0x71 0x11                                                 ;; 0x202: add V1, 0x11
                      0x00 0x00]                                                ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 1 0x53)
                         (assoc :PC 0x204))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "When adding a value, it should overflow without affecting carry register"
      (let [program  [0x61 0x42                                                 ;; 0x200: mov V1, 0x42
                      0x71 0xBF                                                 ;; 0x202: add V1, 0xBE = 0x101
                      0x00 0x00]                                                ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x1)
                         (assoc :PC 0x204))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "When adding a register, it should overflow affecting carry register"
      (let [program  [0x61 0x42                                                 ;; 0x200: mov V1, 0x42
                      0x62 0xBF                                                 ;; 0x202: mov V2, 0xBF
                      0x81 0x24                                                 ;; 0x204: add V1, V2 = 0x101
                      0x00 0x00]                                                ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x1, 0x2 0xBF, 0xF 0x1)
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "When substracting a register, it should overflow affecting carry register to 0 if overflow"
      (let [program  [0x61 0x42                                                 ;; 0x200: mov V1, 0x42
                      0x62 0xBF                                                 ;; 0x202: mov V2, 0xBF
                      0x81 0x25                                                 ;; 0x204: sub V1, V2 = 0x101
                      0x00 0x00]                                                ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x83, 0x2 0xBF, 0xF 0x0)
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "When substracting a register, it should overflow affecting carry register to 1 if no overflow"
      (let [program  [0x61 0xBF                                                 ;; 0x200: mov V1, 0xBF
                      0x62 0x42                                                 ;; 0x202: mov V2, 0x42
                      0x81 0x25                                                 ;; 0x204: sub V1, V2 = 0x101
                      0x00 0x00]                                                ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x7D 0x2 0x42, 0xF 0x1)
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "SUBN should Set Vx = Vy - Vx, set VF = NOT borrow"
      (let [program  [0x61 0x42                                                 ;; 0x200: mov V1, 0x42
                      0x62 0xBF                                                 ;; 0x202: mov V2, 0xBF
                      0x81 0x27                                                 ;; 0x204: sub V1, V2 = 0x101
                      0x00 0x00]                                                ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x7D 0x2 0xBF, 0xF 0x1)
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "register can be ORed against another register"
      (let [program  [0x61 0x0F                                                 ;; 0x200: mov V1, 0x0F
                      0x62 0xF0                                                 ;; 0x202: mov V2, 0xF0
                      0x81 0x21                                                 ;; 0x204: or V1, V2 = 0x101
                      0x00 0x00]                                                ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0xFF, 0x2 0xF0)
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "register can be ANDed against another register"
      (let [program  [0x61 0x0F                                                 ;; 0x200: mov V1, 0x0F
                      0x62 0xF0                                                 ;; 0x202: mov V2, 0xF0
                      0x81 0x22                                                 ;; 0x204: and V1, V2 = 0x101
                      0x00 0x00]                                                ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x00, 0x2 0xF0)
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "register can be XORed against another register"
      (let [program  [0x61 0xAB                                                 ;; 0x200: mov V1, 0xAB
                      0x62 0x55                                                 ;; 0x202: mov V2, 0x55
                      0x81 0x23                                                 ;; 0x204: xor V1, V2
                      0x00 0x00]                                                ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0xFE, 0x2 0x55)
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "shift right a register, overflowing in VF"
      (let [program  [0x61 0x03                                                 ;; 0x200: mov V1, 0x03
                      0x81 0xF6                                                 ;; 0x202: shr V1
                      0x00 0x00]                                                ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x01, 0xF 0x01)
                         (assoc :PC 0x204))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "shift left a register, overflowing in VF"
      (let [program  [0x61 0xFF                                                 ;; 0x200: mov V1, 0xFF
                      0x81 0x0E                                                 ;; 0x202: shl V1
                      0x00 0x00]                                                ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0xFE, 0xF 0x01)
                         (assoc :PC 0x204))
            actual   (launch program)]
        (is (= actual expected)))))
  (testing "Timers"
    (testing "LD ST, V1 Should set the sound timer to Vx."
      (let [program  [0x61 0x03                                                 ;; 0x200: mov V1, 0x03
                      0xF1 0x18                                                 ;; 0x202: ld ST, V1
                      0x00 0x00]                                                ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x03)
                         (assoc :sound-timer (dec 0x03))                        ;; the timer value is decremented because its value is decremented at each clock cycle
                         (assoc :PC 0x204))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "LD DT, V should Set the delay timer to Vx."
      (let [program  [0x61 0x03                                                 ;; 0x200: mov V1, 0x03
                      0xF1 0x15                                                 ;; 0x202: ld DT, V1
                      0x00 0x00]                                                ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x03)                     ;; the timer value is decremented because its value is decremented at each clock cycle
                         (assoc :delay-timer (dec 0x03))
                         (assoc :PC 0x204))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "VX is set to delay timer value"
      (let [program  [0x61 0x03                                                 ;; 0x200: mov V1, 0x03
                      0xF1 0x15                                                 ;; 0x202: ld DT, V1
                      0xF2 0x07                                                 ;; 0x204: mov V2, DT
                      0x00 0x00]                                                ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0x03, 0x02 (dec 0x03))
                         (assoc :delay-timer (dec (dec 0x03)))                  ;; the timer value is decremented because its value is decremented at each clock cycle
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "Read registers V0 through Vx from memory starting at location I."
      (let [program  [0xA2 0x00                                                 ;; 0x200: mov I, 0x200
                      0xF4 0x65                                                 ;; 0x202: mov V4, [I]
                      0x00 0x00]                                                ;; 0x204: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc
                                 0x0 0xA2, 0x01 00, 0x2 0xF4, 0x3 0x65)
                         (assoc :I 0x200)
                         (assoc :PC 0x204))
            actual   (launch program)]
        (is (= actual expected)))))
  (testing "Memory"
    (testing "Store BCD representation of Vx in memory locations I, I+1, and I+2."
      (let [program  [0xA2 0x50                                                 ;; 0x200: mov I, 0x250
                      0x61 0xFF                                                 ;; 0x202: mov V1, 0xFF
                      0xF1 0x33                                                 ;; 0x204 bcd V1
                      0x00 0x00]                                                ;; 0x206: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x1 0xFF)
                         (assoc :I 0x250)
                         (update :RAM assoc 0x250 2 0x251 5 0x252 5)
                         (assoc :PC 0x206))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "Store registers V0 through Vx in memory starting at location I."
      (let [program  [0xA2 0x50                                                 ;; 0x200: mov I, 0x250
                      0x60 0x01                                                 ;; 0x202: mov V0, 0x01
                      0x61 0x02                                                 ;; 0x204: mov V1, 0x02
                      0x62 0x03                                                 ;; 0x206: mov V2, 0x03
                      0x63 0x04                                                 ;; 0x208: mov V3, 0x04
                      0xF4 0x55                                                 ;; 0x20A: mov [I], 4
                      0x00 0x00]                                                ;; 0x20C: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x0 0x01, 0x1 0x02, 0x2 0x03, 0x3 0x04)
                         (assoc :I 0x250)
                         (update :RAM assoc 0x250 0x01, 0x251 0x02, 0x252 0x03, 0x253 0x04)
                         (assoc :PC 0x20C))
            actual   (launch program)]
        (is (= actual expected)))))
  (testing "Drawing"
    (testing "Should draw sprite at (x,y) overflowing vertically and
    horizontally setting VF to 0 because no pixel is unset"
      (let [program  [0x67 0x08                                                 ;; 0x200: mov V7, 8
                      0xF7 0x29                                                 ;; 0x202: mov I, sprite[V7]
                      0x6A 0x3E                                                 ;; 0x204: mov VA, 0x3E (penultimate column)
                      0x6B 0x1E                                                 ;; 0x206: mov VB, 0x1E (penultimate line)
                      0xDA 0xB5                                                 ;; 0x208: draw VA, VB, 5 (draw at [62, 30] the sprite of 5 bytes at I)
                      0x00 0x00]                                                ;; 0x20A: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc 0x7 0x08, 0xA 0x3E, 0xB 0x1E, 0xF 0)
                         (assoc :I 0x28)
                         (assoc :PC 0x20A)
                         (assoc :screen
                                (to-bit-array
                                  [
                                   "1100000000000000000000000000000000000000000000000000000000000011"
                                   "0100000000000000000000000000000000000000000000000000000000000010"
                                   "1100000000000000000000000000000000000000000000000000000000000011"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "0000000000000000000000000000000000000000000000000000000000000000"
                                   "1100000000000000000000000000000000000000000000000000000000000011"
                                   "0100000000000000000000000000000000000000000000000000000000000010"])
                                ))
            actual   (launch program)]
        (is (= actual expected))))
    (testing
      "Printing the same sprite twice result in a blank screen and
  VF is set to 1 because some pixels are unset"
      (let [program  [0x67 0x08                                                 ;; 0x200: mov V7, 8
                      0xF7 0x29                                                 ;; 0x202: mov I, sprite[V7]
                      0x6A 0x3E                                                 ;; 0x204: mov VA, 0x3E (penultimate column)
                      0x6B 0x1E                                                 ;; 0x206: mov VB, 0x1E (penultimate line)
                      0xDA 0xB5                                                 ;; 0x208: draw VA, VB, 5 (draw at [62, 30] the sprite of 5 bytes at I)
                      0xDA 0xB5                                                 ;; 0x20A: draw VA, VB, 5 (draw at [62, 30] the sprite of 5 bytes at I)
                      0x00 0x00]                                                ;; 0x20C: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc
                                 0x7 0x08, 0xA 0x3E, 0xB 0x1E, 0xF 1)
                         (assoc :I 0x28)
                         (assoc :PC 0x20C))
            actual   (launch program)]
        (is (= actual expected))))
    (testing "Should clear the screen but not VF"
      (let [program  [0x67 0x08                                                 ;; 0x200: mov V7, 8
                      0xF7 0x29                                                 ;; 0x202: mov I, sprite[V7]
                      0x6A 0x3E                                                 ;; 0x204: mov VA, 0x3E (penultimate column)
                      0x6B 0x1E                                                 ;; 0x206: mov VB, 0x1E (penultimate line)
                      0xDA 0xB5                                                 ;; 0x208: draw VA, VB, 5 (draw at [62, 30] the sprite of 5 bytes at I)
                      0xDA 0xB5                                                 ;; 0x20A: draw VA, VB, 5 (draw at [62, 30] the sprite of 5 bytes at I)
                      0x00 0xE0                                                 ;; 0x20C: draw VA, VB, 5 (draw at [62, 30] the sprite of 5 bytes at I)
                      0x00 0x00]                                                ;; 0x20A: halt
            expected (-> (load-program fresh-machine program)
                         (update :registers assoc
                                 0x7 0x08, 0xA 0x3E, 0xB 0x1E, 0xF 0x01)
                         (assoc :I 0x28)
                         (assoc :PC 0x20E))
            actual   (launch program)]
        (is (= actual expected))))))
