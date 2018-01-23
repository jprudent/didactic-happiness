(ns repicene.bench-suite
  (:require [clojure.test :refer :all]
            [repicene.cpu-protocol :as cpu]
            [repicene.assembler :as asm]
            [criterium.core :as criterium]
            [repicene.core :as core]))

(defn repeat-bycode [n bytecode]
  (apply concat (repeat n bytecode)))

(defn instr-pg [pg]
  (let [bytecode (asm/assemble pg)
        times    (quot 0x7FFF (count bytecode))
        padding  (rem 0x7FFF (count bytecode))]
    (-> (repeat-bycode times bytecode)
        (concat
          (repeat-bycode padding (asm/assemble "nop"))
          (asm/assemble "halt"))
        (cpu/new-cpu))))

(defmacro my-time
  "Evaluates expr and prints the time it took.  Returns the value of
 expr."
  {:added "1.0"}
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr
         stop# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]
     (prn (str "Elapsed time: " stop# " msecs"))
     (assoc ret# :perf stop#)))

(deftest bench
  (testing "nop"
    (let [cpu (instr-pg "nop")]
      (dotimes [x 100]
        (let [{:keys [perf clock]} (my-time (core/cpu-loop cpu))
              instr-per-second (/ (* 1000 clock) perf)]
          (println clock "instructions")
          (println "in" perf "ms")
          (println "so," instr-per-second "instructions/s"))))))