(ns repicene.test-rom-suite
  (:require [clojure.test :refer :all]
            [repicene.schema :as s]
            [repicene.file-loader :as file-loader]
            [repicene.core :as repicene]
            [clojure.core.async :as async]
            [clojure.core.async :as async]
            [clojure.string :as str]
            [repicene.cpu-protocol :as cpu]))


;; Read roms/cpu_instrs/individual/01-special.gb

#_(do (require '[clojure.tools.namespace.repl :refer [refresh refresh-all]])
      (set! *assert* false)
      (set! *unchecked-math* true)
      (refresh-all))

(defn test-rom
  "At 0xC7D2 is an infinite loop : Jmp 0. This instruction is not hardcoded,
  it's generated by the rom. So I use a memory bp to override this infinite
  loop with a halt instruction"
  [path seconds]
  (let [cpu           (-> (vec (take 0x8000 (file-loader/load-rom path)))
                          (cpu/new-cpu)
                          (cpu/set-pc 0x100))
        serial-output (async/go-loop [buffer ""]
                        (if (str/ends-with? buffer "Passed\n")
                          buffer
                          (recur (str buffer (char (async/<! (:serial-sent-chan cpu)))))))
        looping-cpu   (async/thread (try (repicene/cpu-loop cpu) (catch Exception e e)))
        serial-or-nil (first (async/alts!! [serial-output (async/timeout (* 1000 seconds))]))]
    (println "killing the gameboy")
    (async/put! (:debug-chan-rx cpu) ::s/kill)
    (println "wait kill")
    (println (ex-data (async/<!! looping-cpu)))
    (or serial-or-nil :timeout)))

(def blank (-> (take 0x8000 (repeat 0))
               (vec)
               (cpu/new-cpu)
               (cpu/set-word-at 0x7FFF 0x76)))

(defn run [] (repicene/cpu-loop blank))

(deftest test-01
  (time (is (= "01-special\n\n\nPassed\n"
               (test-rom "roms/cpu_instrs/individual/01-special.gb" 4)))))

(deftest test-02
  (is (= "02-interrupts\n\n\nPassed\n"
         (test-rom "roms/cpu_instrs/individual/02-interrupts.gb" 20))))

(deftest test-03
  (is (= "03-op sp,hl\n\n\nPassed\n"
         (test-rom "roms/cpu_instrs/individual/03-op sp,hl.gb" 20))))

(deftest test-04
  (is (= "04-op r,imm\n\n\nPassed\n"
         (test-rom "roms/cpu_instrs/individual/04-op r,imm.gb" 20))))

(deftest test-05
  (is (= "05-op rp\n\n\nPassed\n"
         (test-rom "roms/cpu_instrs/individual/05-op rp.gb" 20))))

(deftest test-06
  (is (= "06-ld r,r\n\n\nPassed\n"
         (test-rom "roms/cpu_instrs/individual/06-ld r,r.gb" 20))))

(deftest test-07
  (is (= "07-jr,jp,call,ret,rst\n\n\nPassed\n"
         (test-rom "roms/cpu_instrs/individual/07-jr,jp,call,ret,rst.gb" 20))))

(deftest test-08
  (is (= "08-misc instrs\n\n\nPassed\n"
         (test-rom "roms/cpu_instrs/individual/08-misc instrs.gb" 20))))

(deftest test-09
  (is (= "09-op r,r\n\n\nPassed\n"
         (test-rom "roms/cpu_instrs/individual/09-op r,r.gb" 20))))

(deftest test-10
  (is (= "10-bit ops\n\n\nPassed\n"
         (test-rom "roms/cpu_instrs/individual/10-bit ops.gb" 20))))

(deftest test-11
  (is (= "11-op a,(hl)\n\n\nPassed\n"
         (test-rom "roms/cpu_instrs/individual/11-op a,(hl).gb" 20))))