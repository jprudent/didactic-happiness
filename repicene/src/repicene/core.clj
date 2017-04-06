(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]
            [repicene.debug :refer [process-debug-command]]
            [repicene.decoder :refer [pc fetch decoder hex16]]
            [repicene.instructions :refer [exec]]
            [clojure.core.async :refer [go >! chan poll! <!! thread]]
            [repicene.schema :as s]
            [repicene.cpu :refer [cpu-cycle start-debugging]]))

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
     :x-breakpoints         #{}
     :x-once-breakpoints    #{}
     :w-breakpoints         #{}
     ::s/history            nil}))

(defn x-bp? [{:keys [x-breakpoints] :as cpu}]
  (x-breakpoints (pc cpu)))

(defn- debug-session [{:keys [debugging? debug-chan-rx] :as cpu}]
  (if debugging?
    (recur (process-debug-command cpu (<!! debug-chan-rx)))
    cpu))

(defn process-breakpoint [{:keys [debug-chan-tx] :as cpu}]
  (go (>! debug-chan-tx {:command :break}))
  (debug-session (start-debugging cpu)))

(defn process-once-breakpoint [cpu x-once-bps]
  (->> (update cpu :x-once-breakpoints #(apply disj %1 x-once-bps))
       (process-breakpoint)))

(defn x-once-bp [{:keys [x-once-breakpoints] :as cpu}]
  (not-empty (filter #(% cpu) x-once-breakpoints)))

(defn cpu-loop [{:keys [debug-chan-rx] :as cpu}]
  (let [command    (poll! debug-chan-rx)
        x-once-bps (x-once-bp cpu)]
    (recur
      (cond-> cpu
              command (process-debug-command command)
              (x-bp? cpu) (process-breakpoint)
              x-once-bps (process-once-breakpoint x-once-bps)
              :always (cpu-cycle)))))

(defn demo-gameboy []
  (->
    (load-rom "roms/cpu_instrs/cpu_instrs.gb")
    (new-cpu)
    (pc 0x100)
    (update-in [:x-breakpoints] conj 0x77F)
    (update-in [:w-breakpoints] conj 0xFF01)))

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