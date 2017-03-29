(ns repicene.core-test
  (:require [clojure.test :refer :all]
            [repicene.core :refer :all]
            [repicene.decoder :refer :all]
            [repicene.schema :as s]
            [repicene.file-loader :refer [load-rom]]))

(defn to-bytecode [asm]
  (condp = asm
    "ld a,l" [0x7D]
    "ld a,h" [0x7C]
    ["ldh [FF00+n],a" 0x42] [0xE0 0x42]
    ["ld a,0x77", 0x77] [0x3E 0x77]))

(defn compile [program]
  (take 0x8000 (concat (mapcat to-bytecode program) (repeat 0))))

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
      (is (= 0x77 (a (cpu-cycle cpu)))))))

(deftest memory
  (testing "memory is persistant"
    (let [cpu (set-word-at (demo-gameboy) 0xFF42 0xAA)]
      (is (= 0xAA (word-at (::s/memory cpu) 0xFF42))))))

(deftest integration
  (testing "instructions"
    (let [cpu (-> (load-rom "roms/cpu_instrs/cpu_instrs.gb")
                  (new-cpu)
                  (pc 0x100))]
      (is (= 0x100 (pc cpu)))
      (cpu-loop cpu))
    #_(is (= 11 (a (cpu-cycle (demo-gameboy)))))))
