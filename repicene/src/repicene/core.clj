(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]
            [repicene.debug :refer [process-debug-command]]
            [repicene.decoder :refer [pc fetch hex16 decoder set-dword-at word-at sp <FF00+n> %16+ %8- dword-at %16inc a <hl> hl z? c? h? n? %8inc %8dec %16dec %8]]
            [repicene.history :as history]
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
  (-> (destination cpu (source cpu))
      (pc (partial + size))))

(defn dec-sp [cpu] (sp cpu (partial %16+ -2)))
(defn push-sp [cpu dword]
  {:pre  [(s/valid? cpu) (s/dword? dword)]
   :post [(s/valid? %)]}
  (let [cpu (dec-sp cpu)]
    (set-dword-at cpu (sp cpu) dword)))                                         ;; beware : the address should be the decremented sp

(defmethod exec :push
  [cpu {[_ dword-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)
          (= (sp cpu) (%16+ 2 (sp %)))
          (= (dword-at % (sp %)) (dword-register %))
          (= (pc %) (%16+ size (pc cpu)))]}
  (-> (push-sp cpu (dword-register cpu))
      (pc (partial %16+ size))))

(defn inc-sp [cpu] (sp cpu (partial %16+ 2)))
(defn pop-sp [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? (second %)) (s/address? (first %))]}
  [(dword-at cpu (sp cpu)) (inc-sp cpu)])

(defmethod exec :pop
  [cpu {[_ dword-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)
          (= (sp cpu) (%16+ -2 (sp %)))
          (= (dword-at cpu (sp cpu)) (dword-register %))
          (= (pc %) (%16+ size (pc cpu)))]}
  (let [[dword cpu] (pop-sp cpu)]
    (-> (dword-register cpu dword)
        (pc (partial %16+ size)))))

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
          (= (%16+ size (pc cpu)) (dword-at % (sp %)))]}
  (call cpu (constantly true) address size))

(defmethod exec :ret [cpu {[_ cond] :asm, size :size}]
  (if (cond cpu)
    (let [[return-address cpu] (pop-sp cpu)]
      (pc cpu return-address))
    (sp cpu (partial %16+ size))))

(defmethod exec :inc16 [cpu {[_ dword-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(= (%16+ 1 (dword-register cpu)) (dword-register %))
          (= (pc %) (%16+ size (pc cpu)))]}
  (-> (dword-register cpu %16inc)
      (pc (partial %16+ size))))

(defn low-nibble [word]
  {:pre  [(s/word? word)]
   :post [(s/nibble? %)]}
  (bit-and word 0xF))

(defmethod exec :inc [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (let [value  (word-register cpu)
        result (%8inc value)]
    (-> (word-register cpu result)
        (z? (zero? result))
        (n? false)
        (h? (> 0xF (inc (low-nibble value))))
        (pc (partial %16+ size)))))

(defmethod exec :dec [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (let [value  (word-register cpu)
        result (%8dec value)]
    (-> (word-register cpu result)
        (z? (zero? result))
        (n? true)
        (h? (> 0xF (inc (low-nibble value))))
        (pc (partial %16+ size)))))

(defmethod exec :ldi [cpu {[_ destination source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)
          (= (%16inc (hl cpu)) (hl %))
          (= (pc %) (%16+ size (pc cpu)))]}
  (-> (destination cpu (source cpu))
      (hl %16inc)
      (pc (partial %16+ size))))

(defmethod exec :ldd [cpu {[_ destination source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)
          (= (%16dec (hl cpu)) (hl %))
          (= (pc %) (%16+ size (pc cpu)))]}
  (-> (destination cpu (source cpu))
      (hl %16dec)
      (pc (partial %16+ size))))

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
    (pc cpu (partial %16+ size jump))))

(defmethod exec :or [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)
          (= (a %) (bit-or (a cpu) (word-register cpu)))
          (= (pc %) (%16+ size (pc cpu)))]}
  (let [value (bit-or (a cpu) (word-register cpu))]
    (-> (a cpu value)
        (z? (zero? value))
        (pc (partial %16+ size)))))

(defn sub-a [cpu source]
  (let [left-operand  (source cpu)
        right-operand (a cpu)]
    (println "sub" right-operand left-operand (%8- right-operand left-operand))
    (-> (a cpu (%8- right-operand left-operand))
        (z? (= right-operand left-operand))
        (c? (< right-operand left-operand))
        (h? (< (low-nibble right-operand) (low-nibble left-operand)))
        (n? true))))

(defmethod exec :sub [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (-> (sub-a cpu word-register)
      (pc (partial %16+ size))))

(defmethod exec :cp [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %) (= (a cpu) (a %))]}
  (-> (sub-a cpu source)
      (a (a cpu))                                                               ;;restore a register (throw away the result)
      (pc (partial %16+ size))))

(defmethod exec :stop [cpu {size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %) (= ::s/stopped (::s/mode %))]}
  (-> (assoc cpu ::s/mode ::s/stopped)
      (pc (partial %16+ size))))

(defmethod exec :and [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (let [result (bit-and (source cpu) (a cpu))]
    (-> (a cpu result)
        (z? (= 0 result))
        (n? false)
        (h? true)
        (c? false)
        (pc (partial %16+ size)))))

(defmethod exec :add [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (let [x      (source cpu)
        y      (a cpu)
        result (%8 + x y)]
    (-> (a cpu result)
        (z? (= 0 result))
        (n? false)
        (h? (> (+ (low-nibble y) (low-nibble x)) 0xF))
        (c? (> (+ x y) 0xFF))
        (pc (partial %16+ size)))))

(defmethod exec :xor [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (let [result (bit-xor (source cpu) (a cpu))]
    (-> (a cpu result)
        (z? (= 0 result))
        (n? false)
        (h? false)
        (c? false)
        (pc (partial %16+ size)))))




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
    (update-in [:x-breakpoints] conj 0x7CC)))

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