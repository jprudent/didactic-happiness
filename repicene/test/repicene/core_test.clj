(ns repicene.core-test
  (:require [clojure.test :refer :all]
            [repicene.core :refer :all]
            [repicene.decoder :refer :all]
            [repicene.debug :refer :all]
            [repicene.schema :as s]
            [repicene.history :as history]
            [repicene.file-loader :refer [load-rom]]
            [repicene.cpu :refer [cpu-cycle]]
            [clojure.spec.gen :as gen]
            [clojure.spec :as spec]
            [clojure.spec.test :as stest]
            [clojure.core.async :refer [offer! <! >! >!! <!! go poll! chan go alts!! timeout]]
            [repicene.cpu-protocol :as cpu]))

(defn to-bytecode [asm]
  (condp = asm
    "ld a,l" [0x7D]
    "ld a,h" [0x7C]
    ["ldh [FF00+n],a" 0x42] [0xE0 0x42]
    ["ld a,0x77", 0x77] [0x3E 0x77]
    ["jr 0x04", 0x04] [0x18 0x04]
    ["jr 0xFE", 0xFE] [0x18 0xFE]
    "call 0x0004" [0xCD 0x04 0x00]
    "ret" [0xC9]
    "nop" [0x00]
    "push hl" [0xe5]
    "cp 0x90" [0xFE 0x90]
    "ld l,[hl]", [0x6E]
    "sub a,0x05", [0xD6 0x05]))

(defn complete-program [bytes]
  (take 0x8000 (concat bytes (repeat 0))))

(defn compile-prog [program]
  (vec (take 0x8000 (concat (mapcat to-bytecode program) (repeat 0)))))


(deftest instructions
    (testing "ld a,l"
      (let [cpu (-> (compile-prog ["ld a,l"])
                    (cpu/new-cpu)
                    (hl 0x0B8F))]
        (is (= 0x8F (a (cpu-cycle cpu))))))
    (testing "ld a,h"
      (let [cpu (-> (compile-prog ["ld a,h"])
                    (cpu/new-cpu)
                    (hl 0x0B8F))]
        (is (= 0x0B (a (cpu-cycle cpu))))))

    (testing "ldh [FF00+n],a"
      (let [cpu (-> (compile-prog [["ldh [FF00+n],a", 0x42]])
                    (cpu/new-cpu)
                    (cpu/set-word-at 0xFF42 0xAA)
                    (a 0xBB))]
        (is (= 0xAA (cpu/word-at cpu 0xFF42)))
        (is (= 0xAA (repicene.decoder/<FF00+n> cpu)))
        (is (= 0xBB (cpu/word-at (cpu-cycle cpu) 0xFF42)))))
    (testing "ld a,0x77"
      (let [cpu (-> (compile-prog [["ld a,0x77", 0x77]])
                    (cpu/new-cpu)
                    (a 0xBB))]
        (is (= 0xBB (a cpu)))
        (is (= 0x77 (a (cpu-cycle cpu))))))
    (testing "jr r8 (positive)"
      (let [cpu (-> (compile-prog [["jr 0x04", 0x04]])
                    (cpu/new-cpu))]
        (is (= 0x06 (cpu/get-pc (cpu-cycle cpu))))))
    (testing "jr r8 (negative)"
      (let [cpu (-> (compile-prog [["jr 0xFE", 0xFE]])
                    (cpu/new-cpu))]
        (is (= 0x00 (cpu/get-pc (cpu-cycle cpu))))))
    (testing "call ret"
      (let [cpu (-> (compile-prog ["call 0x0004"
                              "nop"
                              "ret"])
                    (cpu/new-cpu))]
        (is (= 0x00 (cpu/get-pc cpu)))
        (is (= 0x04 (cpu/get-pc (cpu-cycle cpu)))
            "call 0x0004 jumps to ret instruction")
        (is (= 0x03 (cpu/get-pc (cpu-cycle (cpu-cycle cpu))))
            "ret jumps back to the nop right after call")))
    (testing "push hl"
      (let [cpu (-> (compile-prog ["push hl"])
                    (cpu/new-cpu)
                    (hl 0xABCD)
                    (sp 0xE000))]
        (is (= 0xABCD (hl cpu)))
        (is (= 0xE000 (sp cpu)))
        (let [cpu-afer-push (cpu-cycle cpu)]
          (is (= 0xDFFE (sp cpu-afer-push)))
          (is (= 0xABCD (dword-at cpu-afer-push 0xDFFE))))))
    (testing "cp 0x90"
      (let [cpu (-> (compile-prog ["cp 0x90"])
                    (cpu/new-cpu)
                    (a 0xAA))]
        (is (= 0xAA (a cpu)))
        (let [cpu-afer-cp (cpu-cycle cpu)]
          (is (= 0xAA (a cpu-afer-cp))))))
    (testing "ld l,[hl]"
      (let [cpu (-> (compile-prog ["ld l,[hl]"])
                    (cpu/new-cpu)
                    (hl 0x8000)
                    (cpu/set-word-at 0x8000 0xEE))]
        (is (= 0x8000 (hl cpu)))
        (is (= 0xEE (cpu/word-at cpu 0x8000)))
        (let [cpu-after-ld (cpu-cycle cpu)]
          (is (= 0xEE (l cpu-after-ld))))))
    (testing "sub a,0x05"
      (let [cpu (-> (compile-prog ["sub a,0x05"])
                    (cpu/new-cpu)
                    (a 0x03))]
        (is (= 0x03 (a cpu)))
        (is (= 0xFE (a (cpu-cycle cpu)))))))

(deftest memory
  (testing "memory is persistant"
    (let [cpu (cpu/set-word-at (demo-gameboy) 0xFF42 0xAA)]
      (is (= 0xAA (cpu/word-at cpu 0xFF42))))))

(deftest history
  (testing "back in history"
    (let [cpu0 (-> (take 0x8000 (load-rom "roms/cpu_instrs/cpu_instrs.gb"))
                   (cpu/new-cpu)
                   (cpu/set-pc 0x100))
          cpu1 (cpu-cycle cpu0)
          cpu2 (cpu-cycle cpu1)]
      (is (= cpu1 (history/restore! cpu2)))
      (is (= cpu0 (history/restore! cpu1)))
      (is (= cpu0 (history/restore! cpu0)) "When history is empty it returns the same cpu"))))

(deftest completeness
  (testing "all instructions can be decoded"
    (doseq [i (range 256)]
      (is (not (nil? (decoder i))) (str "instruction " (hex8 i) " is decoded"))))
  (testing "all extra instructions can be decoded"
    (doseq [i (range 256)]
      (is (not (nil? (extra-decoder i))) (str "instruction " (hex8 i) " is decoded")))))

(defn random-program [size]
  (-> (spec/coll-of ::s/word :count size)
      spec/gen
      gen/sample
      first
      complete-program))

;;todo this is sloooow
(deftest decompiler
  (testing "it can decompile any bytes"
    (let [cpu (-> (random-program 0x8000)
                  (cpu/new-cpu))]
      (doseq [decoded (take 50 (decode-from cpu))]
        (is (spec/valid? ::s/disassembled decoded)
            (spec/explain-str ::s/disassembled decoded))))))

