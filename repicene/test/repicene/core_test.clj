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
            [clojure.core.async :refer [offer! <! >! >!! <!! go poll! chan go alts!! timeout]]))

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

(defn compile [program]
  (vec (take 0x8000 (concat (mapcat to-bytecode program) (repeat 0)))))


(deftest instructions
    (testing "ld a,l"
      (let [cpu (-> (compile ["ld a,l"])
                    (new-cpu)
                    (hl 0x0B8F))]
        (is (= 0x8F (a (cpu-cycle cpu))))))
    (testing "ld a,h"
      (let [cpu (-> (compile ["ld a,h"])
                    (new-cpu)
                    (hl 0x0B8F))]
        (is (= 0x0B (a (cpu-cycle cpu))))))

    (testing "ldh [FF00+n],a"
      (let [cpu (-> (compile [["ldh [FF00+n],a", 0x42]])
                    (new-cpu)
                    (set-word-at 0xFF42 0xAA)
                    (a 0xBB))]
        (is (= 0xAA (word-at (::s/memory cpu) 0xFF42)))
        (is (= 0xAA (repicene.decoder/<FF00+n> cpu)))
        (is (= 0xBB (word-at (::s/memory (cpu-cycle cpu)) 0xFF42)))))
    (testing "ld a,0x77"
      (let [cpu (-> (compile [["ld a,0x77", 0x77]])
                    (new-cpu)
                    (a 0xBB))]
        (is (= 0xBB (a cpu)))
        (is (= 0x77 (a (cpu-cycle cpu))))))
    (testing "jr r8 (positive)"
      (let [cpu (-> (compile [["jr 0x04", 0x04]])
                    (new-cpu))]
        (is (= 0x06 (pc (cpu-cycle cpu))))))
    (testing "jr r8 (negative)"
      (let [cpu (-> (compile [["jr 0xFE", 0xFE]])
                    (new-cpu))]
        (is (= 0x00 (pc (cpu-cycle cpu))))))
    (testing "call ret"
      (let [cpu (-> (compile ["call 0x0004"
                              "nop"
                              "ret"])
                    (new-cpu))]
        (is (= 0x00 (pc cpu)))
        (is (= 0x04 (pc (cpu-cycle cpu)))
            "call 0x0004 jumps to ret instruction")
        (is (= 0x03 (pc (cpu-cycle (cpu-cycle cpu))))
            "ret jumps back to the nop right after call")))
    (testing "push hl"
      (let [cpu (-> (compile ["push hl"])
                    (new-cpu)
                    (hl 0xABCD)
                    (sp 0xE000))]
        (is (= 0xABCD (hl cpu)))
        (is (= 0xE000 (sp cpu)))
        (let [cpu-afer-push (cpu-cycle cpu)]
          (is (= 0xDFFE (sp cpu-afer-push)))
          (is (= 0xABCD (dword-at cpu-afer-push 0xDFFE))))))
    (testing "cp 0x90"
      (let [cpu (-> (compile ["cp 0x90"])
                    (new-cpu)
                    (a 0xAA))]
        (is (= 0xAA (a cpu)))
        (let [cpu-afer-cp (cpu-cycle cpu)]
          (is (= 0xAA (a cpu-afer-cp))))))
    (testing "ld l,[hl]"
      (let [cpu (-> (compile ["ld l,[hl]"])
                    (new-cpu)
                    (hl 0x8000)
                    (set-word-at 0x8000 0xEE))]
        (is (= 0x8000 (hl cpu)))
        (is (= 0xEE (word-at (::s/memory cpu) 0x8000)))
        (let [cpu-after-ld (cpu-cycle cpu)]
          (is (= 0xEE (l cpu-after-ld))))))
    (testing "sub a,0x05"
      (let [cpu (-> (compile ["sub a,0x05"])
                    (new-cpu)
                    (a 0x03))]
        (is (= 0x03 (a cpu)))
        (is (= 0xFE (a (cpu-cycle cpu)))))))

(deftest memory
  (testing "memory is persistant"
    (let [cpu (set-word-at (demo-gameboy) 0xFF42 0xAA)]
      (is (= 0xAA (word-at (::s/memory cpu) 0xFF42))))))

(deftest history
  (testing "back in history"
    (let [cpu0 (-> (load-rom "roms/cpu_instrs/cpu_instrs.gb")
                   (new-cpu)
                   (pc 0x100))
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
  (-> (spec/coll-of ::s/word :distinct true :count size)
      spec/gen
      gen/sample
      first
      complete-program))

;;todo this is sloooow
(deftest decompiler
  (testing "it can decompile any bytes"
    (let [cpu (-> (random-program 100)
                  (new-cpu))]
      (doseq [decoded (take 50 (decode-from cpu))]
        (is (spec/valid? ::s/disassembled decoded)
            (spec/explain-str ::s/disassembled decoded))))))

(defn kill-when-break!
  [cpu]
  (go
    (println "received " (<! (:debug-chan-tx cpu)))
    (>! (:debug-chan-rx cpu) :kill)))

(defn test-rom [path seconds]
  (let [cpu (-> (vec (take 0x8000 (load-rom path)))
                (new-cpu)
                (pc 0x100)
                (set-w-breakpoint 0xFF01
                                  (fn [cpu val]
                                    (println "SERIAL: " (char val))
                                    cpu))
                (set-breakpoint 0xC7D2 :permanent-breakpoint))]
    (is (s/valid? cpu) (spec/explain ::s/cpu cpu))
    (kill-when-break! cpu)
    (let [response-chan (chan)]
      (go
        (time
          (try
            (cpu-loop cpu)
            (catch Exception _
              (>! response-chan true)))))
      (is (first (alts!! [response-chan (timeout (* 1000 seconds))])) path)
      (go (offer! (:debug-chan-rx cpu) :kill)))))

(deftest integration
    (testing "01-specials"
      (test-rom "roms/cpu_instrs/individual/01-special.gb" 60)
      #_(test-rom "roms/cpu_instrs/individual/03-op sp,hl.gb" 600)))
