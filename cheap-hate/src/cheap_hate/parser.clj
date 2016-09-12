(ns cheap-hate.parser
  (:require [cheap-hate.bits-util
             :refer [nth-word lowest-bit highest-bit lowest-byte two-lowest-bytes bit-at]]))

(def w0 ^:private (partial nth-word 4 0))
(def w3 ^:private (partial nth-word 4 3))
(def w1 ^:private (partial nth-word 4 1))
(defn w3-w1-w0 ^:private [opcode] [(w3 opcode) (w1 opcode) (w0 opcode)])
(def address ^:private (partial nth-word 12 0))
(def vx ^:private (partial nth-word 4 2))
(def vy ^:private (partial nth-word 4 1))
(def nn ^:private (partial nth-word 8 0))
(def height ^:private (partial nth-word 4 0))


(defn opcode->instruction
  "extract informations from a 16 bits big-endian opcode."
  [opcode]
  (let [[w3 _ w0 :as w3-w1-w0] (w3-w1-w0 opcode)
        w3-w0 [w3 w0]]
    (mapv #(if (fn? %) (% opcode) %)
          (cond
            (= opcode 0) [:halt]                                                ;; this one, I made up for testing purpose
            (= opcode 0x00E0) [:clear-screen]
            (= opcode 0x00EE) [:return]
            (= 0 w3) [:sys address]
            (= 1 w3) [:jump address]
            (= 2 w3) [:call address]
            (= 3 w3) [:skip-if-value vx '= nn]
            (= 4 w3) [:skip-if-value vx 'not= nn]
            (= [5 0] [w3 w0]) [:skip-if-register vx '= vy]
            (= 6 w3) [:mov-register-value vx nn]
            (= 7 w3) [:add-register-value vx nn]
            (= [8 0] w3-w0) [:mov-register-register vx vy]
            (= [8 1] w3-w0) [:or-register-register vx vy]
            (= [8 2] w3-w0) [:and-register-register vx vy]
            (= [8 3] w3-w0) [:xor-register-register vx vy]
            (= [8 4] w3-w0) [:add-register-register vx vy]
            (= [8 5] w3-w0) [:sub-register-register vx vy]
            (= [8 6] w3-w0) [:shift-right-register vx]
            (= [8 7] w3-w0) [:sub-reverse-register-register vx vy]
            (= [8 0xE] w3-w0) [:shift-left-register vx]
            (= [9 0] w3-w0) [:skip-if-register vx 'not= vy]
            (= 0xA w3) [:mov-i address]
            (= 0xB w3) [:jmp-add-pc-v0 address]
            (= 0xC w3) [:mov-register-random vx nn]
            (= [0xF 1 8] w3-w1-w0) [:mov-sound-timer vx]
            (= [0xF 1 0xE] w3-w1-w0) [:add-i vx]
            (= [0xF 2 9] w3-w1-w0) [:mov-i-font vx]
            (= [0xF 3 3] w3-w1-w0) [:mov-i-decimal vx]
            (= 0xD w3) [:draw vx vy height]
            (= [0xE 9 0xE] w3-w1-w0) [:skip-if-key '= vx]
            (= [0xE 0xA 1] w3-w1-w0) [:skip-if-key 'not= vx]
            (= [0xF 0 7] w3-w1-w0) [:mov-register-delay-timer vx]
            (= [0xF 0 0xA] w3-w1-w0) [:mov-register-key vx]
            (= [0xF 1 5] w3-w1-w0) [:mov-delay-timer vx]
            (= [0xF 5 5] w3-w1-w0) [:set-memory vx]
            (= [0xF 6 5] w3-w1-w0) [:mov-registers-memory vx]))))
