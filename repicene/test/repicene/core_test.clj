(ns repicene.core-test
  (:require [clojure.test :refer :all]
            [repicene.core :refer :all]
            [repicene.decoder :refer :all]
            [repicene.schema :as s]
            [repicene.file-loader :refer [load-rom]]))

(defn to-bytecode [asm]
  (condp = asm
    "ld a,l" [0x7D]
    "ld a,h" [0x7C]))

(defn compile [program]
  (take 0x8000 (concat (mapcat to-bytecode program) (repeat 0))))

(deftest instructions
  (testing "ld a,l"
    (let [cpu (-> (compile ["ld a,l"])
                  (new-cpu)
                  (l 11))]
      (is (= 11 (a (cpu-cycle cpu))))))
  (testing "ld a,h"
      (let [cpu (-> (compile ["ld a,h"])
                    (new-cpu)
                    (h 11))]
        (println (h? cpu))
        (is (= 11 (a (cpu-cycle cpu)))))))

(deftest integration
  (testing "instructions"
    (let [cpu (-> (load-rom "roms/cpu_instrs/cpu_instrs.gb")
        (new-cpu)
        (pc 0x100))]
      (is (= 0x100 (pc cpu)))
      (cpu-loop cpu))
    #_(is (= 11 (a (cpu-cycle (demo-gameboy)))))))
