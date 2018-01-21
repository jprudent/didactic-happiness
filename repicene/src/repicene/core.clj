(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]
            [repicene.debug :refer [process-debug-command process-breakpoint set-breakpoint]]
            [repicene.decoder :refer [exec pc fetch decoder hex16]]
            [clojure.core.async :refer [sliding-buffer go >! chan poll! <!! thread]]
            [repicene.schema :as s]
            [repicene.cpu :refer [cpu-cycle]]
            [repicene.cpu-protocol :as cpu]))

(defn cpu-loop [{:keys [debug-chan-rx clock] :as cpu}]
  {:pre [(s/cpu? cpu)]}
  (let [command (when (= 0xFFFF (bit-and clock 0xFFFF)) (poll! debug-chan-rx))]
    (cond command (do (println "cmd") (recur (process-debug-command cpu command)))
          (cpu/running? cpu) (recur (cpu-cycle cpu))
          (cpu/halted? cpu) cpu
          (cpu/break? cpu) (do (println "brk") (recur (process-breakpoint cpu))))))

(defn demo-gameboy
  ([]
   (->
     (vec (take 0x8000 (load-rom "roms/cpu_instrs/individual/07-jr,jp,call,ret,rst.gb")))
     (cpu/new-cpu)
     (pc 0x100)
     (set-breakpoint 0x0100 :permanent-breakpoint)
     (set-breakpoint 0xFF01 :permanent-breakpoint)))
  ([coredump]
   (let [gameboy (-> (demo-gameboy)
                     (merge (read-string (slurp coredump))))]
     (set-breakpoint gameboy (pc gameboy) :permanent-breakpoint))))