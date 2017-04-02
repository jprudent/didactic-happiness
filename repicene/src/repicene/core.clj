(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]
            [repicene.debug :refer [process-debug-command]]
            [repicene.history :as history]
            [repicene.decoder :refer [pc fetch decoder hex16]]
            [repicene.instructions :refer [exec]]
            [clojure.core.async :refer [go >! chan poll! <!! thread]]
            [repicene.schema :as s]))

(defn new-cpu [rom]
  (let [wram-1 (vec (take 0x1000 (repeat 0)))
        io     (vec (take 0x0080 (repeat 0)))
        hram   (vec (take 0x0080 (repeat 0)))
        vram   (vec (take 0x2000 (repeat 0)))]
    {::s/registers          {::s/AF 0
                             ::s/BC 0
                             ::s/DE 0
                             ::s/HL 0
                             ::s/SP 0
                             ::s/PC 0}
     ::s/interrupt-enabled? true
     ::s/memory             [[0x0000 0x7FFF rom]
                             [0x8000 0x9FFF vram]
                             [0xD000 0xDFFF wram-1]
                             [0xFF00 0xFF7F io]
                             [0xFF80 0xFFFF hram]]
     ::s/mode               ::s/running
     :debug-chan-rx         (chan)
     :debug-chan-tx         (chan)
     :x-breakpoints         []
     ::s/history            '()}))





(defn x-bp? [{:keys [x-breakpoints] :as cpu}]
  (some (partial = (pc cpu)) x-breakpoints))

(defn instruction-at-pc [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(not (nil? %))]}
  (get decoder (fetch cpu)))

(defn cpu-cycle [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? cpu)]}
  (let [instr (instruction-at-pc cpu)
        #__     #_(println "before " (str "@" (hex16 (pc cpu))) ((:to-string instr) cpu))
        ret   (history/save cpu)
        ret   (exec ret instr)]
    ret
    ))


(defn process-breakpoint [{:keys [debug-chan-rx] :as cpu}]
  (println "breakpoint at " (pc cpu))
  (loop [cpu     cpu
         command (<!! debug-chan-rx)]
    (println "while waiting for resume, i received" command)
    (cond
      (= :resume command) cpu
      (= :step-over command) (recur (cpu-cycle cpu) (<!! debug-chan-rx))
      (= :back-step command) (recur (history/restore cpu) (<!! debug-chan-rx))
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
    (pc 0x100)
    (update-in [:x-breakpoints] conj 0x213)))

#_(def cpu
    (->
      (load-rom "roms/cpu_instrs/cpu_instrs.gb")
      (new-cpu)
      (assoc-in [::s/registers :PC] 0x100)))

;; POC BREAKPOINT
#_(do
    (def cpu
      (->
        (load-rom "roms/cpu_instrs/cpu_instrs.gb")
        (new-cpu)
        (assoc-in [::s/registers :PC] 0x100)
        (update-in [:x-breakpoints] conj 0x637)))
    (thread (cpu-loop cpu))
    (async/>!! (debug-chan-tx cpu) "yolo"))