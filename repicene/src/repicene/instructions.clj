(ns repicene.instructions
  (:require [repicene.decoder :refer [pc fetch hex16 decoder set-dword-at word-at sp <FF00+n> %16+ %8- dword-at %16inc a <hl> hl z? c? h? n? %8inc %8dec %16dec %8 %16 extra-decoder low-word]]
            [repicene.schema :as s]
            [clojure.test :refer [is]]))

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
  {:post [(is (= (source cpu) (destination (pc % (pc cpu)))))]}
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
    (pc cpu (partial %16+ size))))

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
    #_(println "or " (a cpu) " " (word-register cpu) " = " value)
    (-> (a cpu value)
        (z? (zero? value))
        (pc (partial %16+ size)))))

(defn sub-a [cpu source]
  (let [y (source cpu)
        x (a cpu)]
    #_(println "sub" x y (%8- x y) "c" (< x y))
    (-> (a cpu (%8- x y))
        (z? (= y x))
        (c? (< x y))
        (h? (< (low-nibble x) (low-nibble y)))
        (n? true))))

(defmethod exec :sub [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (-> (sub-a cpu word-register)
      (pc (partial %16+ size))))

(defn bool->int [b] (if b 1 0))

(defmethod exec :sbc [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (-> (sub-a cpu #(%8 + (word-register cpu) (bool->int (c? cpu))))
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

(defn add [cpu x size]
  (let [y      (a cpu)
        result (%8 + x y)]
    (-> (a cpu result)
        (z? (zero? result))
        (n? false)
        (h? (> (+ (low-nibble y) (low-nibble x)) 0xF))
        (c? (> (+ x y) 0xFF))
        (pc (partial %16+ size)))))

(defmethod exec :add [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (add cpu (source cpu) size))

(defmethod exec :add-hl [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (let [x (hl cpu)
        y (source cpu)]
    (-> (hl cpu (%16+ x y))
        (n? false)
        (h? (> (+ (bit-and x 0x0FFF) (bit-and y 0x0FFF)) 0xFFF))
        (c? (> (+ x y) 0xFFFF))
        (pc (partial %16+ size)))))

(defmethod exec :add-sp [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (let [x (sp cpu)
        y (source cpu)]
    (-> (sp cpu (%16+ x y))
        (z? false)
        (n? false)
        (h? (> (+ (low-nibble y) (low-nibble x)) 0xF))
        (c? (> (+ (low-word x) y) 0xFF))
        (pc (partial %16+ size)))))

(defmethod exec :adc [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (add cpu (%8 + (bool->int (c? cpu)) (source cpu)) size))

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

(defmethod exec :sla [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (let [value  (source cpu)
        result (bit-shift-left value 1)]
    (-> (source cpu result)
        (z? (zero? result))
        (n? false)
        (h? false)
        (c? (bit-test value 7))
        (pc (partial %16+ size)))))

(defmethod exec :srl [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (let [value  (source cpu)
        result (bit-shift-right value 1)]
    (-> (source cpu result)
        (z? (zero? result))
        (n? false)
        (h? false)
        (c? (bit-test value 0))
        (pc (partial %16+ size)))))

(defmethod exec :sra [cpu {[_ source] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (let [value   (source cpu)
        highest (bit-and value 2r10000000)
        result  (-> (bit-shift-right value 1)
                    (bit-or highest))]                                          ;; MSB doesn't change !
    (-> (source cpu result)
        (z? (zero? result))
        (n? false)
        (h? false)
        (c? (bit-test value 0))
        (pc (partial %16+ size)))))



(defn rotate-left [word]
  {:pre  [(s/word? word)]
   :post [(s/word? %)]}
  (let [highest (bool->int (bit-test word 7))]
    (-> (bit-shift-left word 1)
        (bit-or highest))))

(defn rlc [cpu word-register size]
  (let [x      (word-register cpu)
        result (rotate-left x)]
    (-> (word-register cpu result)
        (z? (zero? result))
        (n? false)
        (h? false)
        (c? (bit-test x 7))
        (pc (partial %16+ size)))))

;; rotate left, Old bit 7 to Carry flag.
(defmethod exec :rlc [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (rlc cpu word-register size))

(defmethod exec :rlca [cpu {size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (-> (rlc cpu a size)
      (z? false)))

(defn rl [cpu source size]
  (let [value  (source cpu)
        result (bit-or (bit-shift-left value 1)
                       (bool->int (c? cpu)))]
    (-> (source cpu result)
        (z? (zero? result))
        (n? false)
        (h? false)
        (c? (bit-test value 7))
        (pc (partial %16+ size)))))

;; rotate left through carry flag
(defmethod exec :rl [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (rl cpu word-register size))

(defmethod exec :rla [cpu {size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (-> (rl cpu a size)
      (z? false)))

(defn rotate-right [word]
  {:pre  [(s/word? word)]
   :post [(s/word? %)]}
  (let [highest (-> (bit-and word 1)
                    (bit-shift-left 7))]
    (-> (bit-shift-right word 1)
        (bit-or highest))))

(defn rrc [cpu word-register size]
  (let [x      (word-register cpu)
        result (rotate-right x)]
    (-> (word-register cpu result)
        (z? (zero? result))
        (n? false)
        (h? false)
        (c? (bit-test x 0))
        (pc (partial %16+ size)))))

;; rotate right, Old bit 7 to Carry flag.
(defmethod exec :rrc [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (rrc cpu word-register size))

(defmethod exec :rrca [cpu {size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (-> (rrc cpu a size)
      (z? false)))

(defn rr [cpu word-register size]
  (let [value  (word-register cpu)
        result (bit-or (bit-shift-right value 1)
                       (bit-shift-left (bool->int (c? cpu)) 7))]
    (-> (word-register cpu result)
        (z? (zero? result))
        (n? false)
        (h? false)
        (c? (bit-test value 0))
        (pc (partial %16+ size)))))

;; rotate right through carry flag
(defmethod exec :rr [cpu {[_ word-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (rr cpu word-register size))

(defmethod exec :rra [cpu {size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (-> (rr cpu a size)
      (z? false)))

(defmethod exec :extra [cpu {[_ opcode] :asm size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (-> (pc cpu (partial %16+ size))
      (exec (extra-decoder (opcode cpu)))))

(defmethod exec :dec16 [cpu {[_ dword-register] :asm, size :size}]
  {:pre  [(s/valid? cpu)]
   :post [(s/valid? %)]}
  (-> (dword-register cpu (%16 dec (dword-register cpu)))
      (pc (partial %16+ size))))