(ns repicene.test-rom-suite
  (:require [clojure.test :refer :all]
            [repicene.schema :as s]
            [repicene.file-loader :as file-loader]
            [repicene.decoder :as decoder]
            [repicene.core :as repicene]
            [clojure.core.async :refer [go <!! >! >!! chan alts!! timeout offer! thread]]
            [clojure.core.async :as async]
            [clojure.string :as str]))


;; Read roms/cpu_instrs/individual/01-special.gb

#_(do (require '[clojure.tools.namespace.repl :refer [refresh refresh-all]])
      (set! *assert* false)
      (refresh-all))

(defn halt-at-0xC7D2 [cpu val]
  (println "0xC7D2 rewritten at " (decoder/hex16 (decoder/pc cpu)))
  (decoder/set-word-at cpu 0xC7D2 0x76))

(defn record-serial [serial-buffer]
  (fn [cpu val]
    (print (char val)) (flush)
    (swap! serial-buffer str (char val))
    cpu))

(defn test-rom
  "At 0xC7D2 is an infinite loop : Jmp 0. This instruction is not hardcoded,
  it's generated by the rom. So I use a memory bp to override this infinite
  loop with a halt instruction"
  [path seconds]
  (let [cpu           (-> (vec (take 0x8000 (file-loader/load-rom path)))
                          (repicene/new-cpu)
                          (decoder/pc 0x100))
        serial-output (async/go-loop [buffer ""]
                        (println "buffer " buffer)
                        (if (str/ends-with? buffer "Passed\n")
                          buffer
                          (recur (str buffer (char (async/<! (:serial-sent-chan cpu)))))))
        looping-cpu   (thread (try (repicene/cpu-loop cpu) (catch Exception e e)))
        serial-or-nil (first (alts!! [serial-output (timeout (* 1000 seconds))]))]
    (println "killing the gameboy")
    (>!! (:debug-chan-rx cpu) ::s/kill)
    (println "wait kill")
    (<!! looping-cpu)
    serial-or-nil))

(def blank (-> (take 0x8000 (repeat 0))
               (vec)
               (repicene/new-cpu)
               (decoder/set-word-at 0x7FFF 0x76)))

(defn run [] (repicene/cpu-loop blank))

(deftest integration
  (testing "cpu instructions"
    (is (= "01-special\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/01-special.gb" 11)))
    (is (= "02-interrupts\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/02-interrupts.gb" 20)))
    (is (= "03-op sp,hl\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/03-op sp,hl.gb" 20)))
    (is (= "04-op r,imm\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/04-op r,imm.gb" 20)))
    (is (= "05-op rp\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/05-op rp.gb" 20)))
    (is (= "06-ld r,r\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/06-ld r,r.gb" 20)))
    (is (= "07-jr,jp,call,ret,rst\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/07-jr,jp,call,ret,rst.gb" 20)))
    (is (= "08-misc instrs\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/08-misc instrs.gb" 20)))
    (is (= "09-op r,r\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/09-op r,r.gb" 20)))
    (is (= "10-bit ops\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/10-bit ops.gb" 20)))
    (is (= "11-op a,(hl)\n\n\nPassed\n"
           (test-rom "roms/cpu_instrs/individual/11-op a,(hl).gb" 20)))))
