(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]
            [repicene.debug :refer [process-debug-command]]
            [repicene.decoder :refer [pc fetch hex16 decoder set-dword-at word-at sp <FF00+n> %+ dword-at %inc a <hl> hl z?]]
            [repicene.history :as history]
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
     :x-breakpoints         []
     ::s/history            '()}))

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
  (let [cpu2 (destination cpu (source cpu))
        cpu2 (pc cpu2 (partial + size))]
    (println "!!! after source" (source (pc cpu2 (pc cpu)))
             "destination" (destination (pc cpu2 (pc cpu))))
    cpu2))

(defn dec-sp [cpu] (sp cpu (partial %+ -2)))
(defn push-sp [cpu dword]
  {:pre  [(s/valid? cpu) (s/dword? dword)]
   :post [(s/valid? %)]}
  (let [cpu (dec-sp cpu)]
    (set-dword-at cpu (sp cpu) dword)))                                         ;; beware : the address should be the decremented sp

(defmethod exec :push
  [cpu {[_ dword-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)
          (= (sp cpu) (%+ 2 (sp %)))
          (= (dword-at % (sp %)) (dword-register %))
          (= (pc %) (%+ size (pc cpu)))]}
  (-> (push-sp cpu (dword-register cpu))
      (pc (partial %+ size))))

(defn inc-sp [cpu] (sp cpu (partial %+ 2)))
(defn pop-sp [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? (second %)) (s/address? (first %))]}
  [(dword-at cpu (sp cpu)) (inc-sp cpu)])

(defmethod exec :pop
  [cpu {[_ dword-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)
          (= (sp cpu) (%+ -2 (sp %)))
          (= (dword-at cpu (sp cpu)) (dword-register %))
          (= (pc %) (%+ size (pc cpu)))]}
  (let [[dword cpu] (pop-sp cpu)]
    (-> (dword-register cpu dword)
        (pc (partial %+ size)))))

(defn- call [cpu cond address size]
  (let [next-pc (+ size (pc cpu))]
    (if (cond cpu)
      (-> (push-sp cpu next-pc)
          (pc address))
      (pc cpu next-pc))))

(defmethod exec :call [cpu {[_ cond address] :asm, size :size}]
  (call cpu cond (address cpu) size))

(defmethod exec :rst [cpu {[_ address] :asm, size :size}]
  {:pre  [(s/valid? cpu) (s/word? address)]
   :post [(s/valid? %)
          (= address (pc %))
          (= (%+ size (pc cpu)) (dword-at % (sp %)))]}
  (call cpu (constantly true) address size))

(defmethod exec :ret [cpu {[_ cond] :asm, size :size}]
  (if (cond cpu)
    (let [[return-address cpu] (pop-sp cpu)]
      (pc cpu return-address))
    (sp cpu (partial %+ size))))

(defmethod exec :inc [cpu {[_ dword-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(= (%+ 1 (dword-register cpu)) (dword-register %))
          (= (pc %) (%+ size (pc cpu)))]}
  (-> (dword-register cpu %inc)
      (pc (partial %+ size))))

(defmethod exec :ldi [cpu {size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)
          (= (a %) (<hl> cpu))
          (= (%inc (hl cpu)) (hl %))
          (= (pc %) (%+ size (pc cpu)))]}
  (-> (a cpu (<hl> cpu))
      (hl %inc)
      (pc (partial %+ size))))

(defn positive? [address]
  (zero? (bit-and address 2r10000000)))

(defn abs "(abs n) is the absolute value of n" [n]
  {:pre [(number? n)]}
  (if (neg? n) (- n) n))

(defn two-complement [word]
  {:pre  [(s/word? word)]
   :post [(<= (abs %) 127)]}
  (if (positive? word)
    word
    (* -1 (bit-and (inc (bit-not word)) 0xFF))))

(defmethod exec :jr [cpu {[_ cond relative-address] :asm, size :size}]
  {:pre  [(s/valid? cpu) (s/word? (relative-address cpu))]
   :post [(s/valid? %)]}
  (let [jump (if (cond cpu) (two-complement (relative-address cpu)) 0)]
    (pc cpu (partial %+ size jump))))

(defmethod exec :or [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)
          (= (a %) (bit-or (a cpu) (word-register cpu)))
          (= (pc %) (%+ size (pc cpu)))]}
  (let [value (bit-or (a cpu) (word-register cpu))]
    (-> (a cpu value)
        (z? (zero? value))
        (pc (partial %+ size)))))

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
        _     (println "before " (str "@" (hex16 (pc cpu))) ((:to-string instr) cpu))
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
    (update-in [:x-breakpoints] conj 0x740)))

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