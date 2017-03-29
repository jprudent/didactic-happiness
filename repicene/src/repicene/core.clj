(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]
            [repicene.debug :refer [process-debug-command]]
            [repicene.decoder :refer [pc fetch hex16 decoder set-dword-at word-at sp <FF00+n>]]
            [clojure.core.async :refer [go >! chan poll! <!! thread]]
            [repicene.schema :as s]))

(defn new-cpu [rom]
  (let [wram-1 (vec (take 0x1000 (repeat 0)))
        io     (vec (take 0x80 (repeat 0)))
        hram   (vec (take 0x80 (repeat 0)))]
    {::s/registers          {::s/AF 0
                             ::s/BC 0
                             ::s/DE 0
                             ::s/HL 0
                             ::s/SP 0
                             ::s/PC 0}
     ::s/interrupt-enabled? true
     ::s/memory             [[0x0000 0x7FFF rom]
                             [0xD000 0xDFFF wram-1]
                             [0xFF00 0xFF7F io]
                             [0xFF80 0xFFFF hram]]
     :debug-chan-rx         (chan)
     :debug-chan-tx         (chan)
     :x-breakpoints         []}))

(defmulti exec (fn [_ {:keys [asm]}] (first asm)))
(defmethod exec :nop [cpu _] (pc cpu inc))
(defmethod exec :jp [cpu {[_ condition address] :asm, size :size}]
  (if (condition cpu)
    (pc cpu (address cpu))
    (pc cpu (partial + size))))
(defmethod exec :di [cpu _]
  (-> (assoc cpu :interrupt-enabled? false)
      (pc inc)))
(defmethod exec :ei [cpu _]
  (-> (assoc cpu :interrupt-enabled? true)
      (pc inc)))

(defmethod exec :ld [cpu {[_ destination source] :asm, size :size}]
  {:post [(= (source (pc % (pc cpu))) (destination (pc % (pc cpu))))]}
  (println
    "!!! before"
    "source " (source cpu)
    "destination " (destination cpu)
    "@" (pc cpu))
  (let [pc-bak (pc cpu)
        cpu2 (destination cpu (source cpu))
        cpu2 (pc cpu2 (partial + size))]
    (println
      "!!! after "
      "source " (source (pc cpu2 pc-bak))
      "destination " (destination cpu2)
      (= (source (pc cpu2 (pc cpu))) (destination cpu2))
      (::s/registers cpu)
      "@" (pc cpu2) (pc (pc cpu2 pc-bak))
      (word-at (::s/memory cpu2) 0xFF42)
      (= <FF00+n> destination)
      (pc (pc cpu2 pc-bak))
      (<FF00+n> (pc cpu2 pc-bak)))
    cpu2))

(defn dec-sp [cpu] (sp cpu (- (sp cpu) 2)))
(defn push [cpu next-pc]
  (let [cpu (dec-sp cpu)]
    (set-dword-at cpu (sp cpu) next-pc)))                                       ;; beware : the address should be the decremented sp

(defmethod exec :call [cpu {[_ cond address] :asm, size :size}]
  (let [next-pc (+ size (pc cpu))
        call?   (cond cpu)]
    (cond-> cpu
            call? (push next-pc)
            call? (pc (address cpu)))))

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
        _     (println "before " (str "@" (hex16 (pc cpu))) ((:to-string instr) cpu) (::s/registers cpu))
        ret   (exec cpu instr)]
    (println "after " (::s/registers ret))
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
    (update-in [:x-breakpoints] conj 0x2A3)))

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