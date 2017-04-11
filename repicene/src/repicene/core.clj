(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]
            [repicene.debug :refer [process-debug-command]]
            [repicene.decoder :refer [pc fetch decoder hex16]]
            [repicene.instructions :refer [exec]]
            [clojure.core.async :refer [sliding-buffer go >! chan poll! <!! thread]]
            [repicene.schema :as s]
            [repicene.cpu :refer [cpu-cycle start-debugging]]))

(defn new-cpu [rom]
  {:pre [(= 0x8000 (count rom))]}
  (let [wram-0    (vec (take 0x1000 (repeat 0)))
        wram-1    (vec (take 0x1000 (repeat 0)))
        io        (vec (take 0x0080 (repeat 0)))
        hram      (vec (take 0x0080 (repeat 0)))
        vram      (vec (take 0x2000 (repeat 0)))
        ext-ram   (vec (take 0x2000 (repeat 0)))
        oam-ram   (vec (take 0x00A0 (repeat 0)))
        echo      (vec (take 0x1E00 (repeat 0)))
        unusable (vec (take 0x0060 (repeat 0)))]
    {::s/registers          {::s/AF 0
                             ::s/BC 0
                             ::s/DE 0
                             ::s/HL 0
                             ::s/SP 0
                             ::s/PC 0}
     ::s/interrupt-enabled? true
     ::s/memory             [[0x0000 0x7FFF rom]
                             [0x8000 0x9FFF vram]
                             [0xA000 0xBFFF ext-ram]
                             [0xC000 0xCFFF wram-0]
                             [0xD000 0xDFFF wram-1]
                             [0xE000 0xFDFF echo]                               ;;todo real echo
                             [0xFE00 0xFE9F oam-ram]
                             [0xFEA0 0xFEFF unusable]
                             [0xFF00 0xFF7F io]
                             [0xFF80 0xFFFF hram]]
     ::s/mode               ::s/running
     :debug-chan-rx         (chan)
     :debug-chan-tx         (chan)
     :history-chan          (chan (sliding-buffer 100))
     ::s/x-breakpoints      #{}
     :x-once-breakpoints    #{}
     :w-breakpoints         #{}
     :debugging?            nil}))

(defn x-bp? [{:keys [::s/x-breakpoints] :as cpu}]
  (x-breakpoints (pc cpu)))

(defn- debug-session [{:keys [debugging? debug-chan-rx] :as cpu}]
  (if debugging?
    (recur (process-debug-command cpu (<!! debug-chan-rx)))
    cpu))

(defn process-breakpoint [{:keys [debug-chan-tx] :as cpu}]
  (go (>! debug-chan-tx {:command :break}))
  (debug-session (start-debugging cpu)))

(defn process-once-breakpoint [cpu x-once-bps]
  (->> (apply update cpu :x-once-breakpoints disj x-once-bps)
       (process-breakpoint)))

(defn x-once-bp [{:keys [x-once-breakpoints] :as cpu}]
  (not-empty (filter #(% cpu) x-once-breakpoints)))

(defn cpu-loop [{:keys [debug-chan-rx] :as cpu}]
  {:pre [(s/valid? cpu)]}
  (let [command    (poll! debug-chan-rx)
        x-once-bps (x-once-bp cpu)]
    (recur
      (cond-> cpu
              command (process-debug-command command)
              (x-bp? cpu) (process-breakpoint)
              x-once-bps (process-once-breakpoint x-once-bps)
              :always (cpu-cycle)))))

(defn demo-gameboy
  ([]
   (->
     (vec (take 0x8000 (load-rom "roms/cpu_instrs/individual/01-special.gb")))
     (new-cpu)
     (pc 0x100)
     (update-in [::s/x-breakpoints] conj 0x100)
     (update-in [:w-breakpoints] conj 0xFF01)))
  ([coredump]
   (let [gameboy (-> (demo-gameboy)
                     (merge (read-string (slurp coredump))))]
     (update-in gameboy [::s/x-breakpoints] conj (pc gameboy)))))

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
        (update-in [::s/x-breakpoints] conj 0x637)))
    (thread (cpu-loop cpu))
    (async/>!! (debug-chan-tx cpu) "yolo"))