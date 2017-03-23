(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]
            [repicene.debug :refer [process-debug-command]]
            [repicene.decoder :refer [pc fetch hex16 decoder]]
            [clojure.core.async :refer [go >! chan poll! <!! thread]]))

;; a word is an 8 bits positive integer
;; a dword is a 16 bits positive integer

(defn new-cpu [rom]
  (let [wram-1 (vec (take 0x1000 (repeat 0)))
        io (vec (take 0x80 (repeat 0)))
        hram (vec (take 0x80 (repeat 0)))]
    {:registers          {:AF 0
                          :BC 0
                          :DE 0
                          :HL 0
                          :SP 0
                          :PC 0}
     :interrupt-enabled? true
     :memory             [[0x0000 0x7FFF rom]
                          [0xD000 0xDFFF wram-1]
                          [0xFF00 0xFF7F io]
                          [0xFF80 0xFFFF hram]]
     :debug-chan-rx      (chan)
     :debug-chan-tx      (chan)
     :x-breakpoints      []}))

(defmulti exec (fn [_ [instr & _]] instr))
(defmethod exec :nop [cpu _] (pc cpu inc))
(defmethod exec :jp [cpu [_ condition address & _]]
  (if (condition cpu)
    (pc cpu (address cpu))
    (pc cpu (partial + 3))))
(defmethod exec :di [cpu _]
  (-> (assoc cpu :interrupt-enabled? false)
      (pc inc)))
(defmethod exec :ei [cpu _]
  (-> (assoc cpu :interrupt-enabled? true)
      (pc inc)))
(defmethod exec :ld [cpu [_ destination source _ size]]
  (-> (destination cpu (source cpu))
      (pc (partial + size))))

(defn x-bp? [{:keys [x-breakpoints] :as cpu}]
  (some (partial = (pc cpu)) x-breakpoints))

(defn cpu-cycle [cpu]
  (let [instr (get decoder (fetch cpu))]
    (println (str "@" (hex16 (pc cpu))) ((last instr) cpu))
    (exec cpu instr)))

(defn process-breakpoint [{:keys [debug-chan-rx] :as cpu}]
  (println "breakpoint at " (pc cpu))
  (loop [cpu     cpu
         command (<!! debug-chan-rx)]
    (println "while waiting for resume, i received" command)
    (cond
      (= :resume command) cpu
      (= :step-over command) (recur (cpu-cycle cpu) (<!! debug-chan-rx))
      :default (recur (process-debug-command cpu command)
                      (<!! debug-chan-rx)))))

(defn cpu-loop [{:keys [debug-chan-rx] :as cpu}]
  (let [command (poll! debug-chan-rx)]
    (recur
      (cond-> cpu
              command (process-debug-command command)
              (x-bp? cpu) (process-breakpoint)
              :always (cpu-cycle)))))

(defn demo-gameboy []
  (->
    (load-rom "roms/cpu_instrs/cpu_instrs.gb")
    (new-cpu)
    (assoc-in [:registers :PC] 0x100)
    (update-in [:x-breakpoints] conj 0x437)))

#_(def cpu
    (->
      (load-rom "roms/cpu_instrs/cpu_instrs.gb")
      (new-cpu)
      (assoc-in [:registers :PC] 0x100)))

;; POC BREAKPOINT
#_(do
    (def cpu
      (->
        (load-rom "roms/cpu_instrs/cpu_instrs.gb")
        (new-cpu)
        (assoc-in [:registers :PC] 0x100)
        (update-in [:x-breakpoints] conj 0x637)))
    (thread (cpu-loop cpu))
    (async/>!! (debug-chan-tx cpu) "yolo"))