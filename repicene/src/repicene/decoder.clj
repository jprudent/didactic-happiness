(ns repicene.decoder
  (:require [repicene.schema :as s :refer [dword? word?]]
            [repicene.bits :refer [two-complement]]
            [clojure.test :refer [is]]
            [repicene.address-alias :as alias]
            [repicene.dada :refer [daa]]
            [clojure.core.async :as async]
            [repicene.cpu-protocol :as cpu])
  (:import (java.io Writer)))

(def hex8
  "Transform a word to it's hexadecimal string representation"
  (memoize (partial format "0x%02X")))

(def hex16
  "Transform a dword to it's hexadecimal string representation"
  (memoize (partial format "0x%04X")))

(defn cat8
  "concatenate two words to make a dword"
  [high low]
  {:pre  [(s/word? high) (s/word? low)]
   :post [(s/dword? %)]}
  (bit-or (bit-shift-left high 8) low))

(defn %16+
  "Add numbers and make it a valid address (mod 0xFFFF)"
  [v1 v2]
  (let [v1   (int v1)
        v2   (int v2)
        mask (int 0xFFFF)]
    (bit-and (+ v1 v2) mask)))

(defn %8
  "Word arithmetic should be 0xFF modular arithmetic"
  [f & args]
  {:post [(s/word? %)]}
  (let [r ^short (apply f args)]
    (if (pos? r)
      (bit-and r 0xFF)
      (-> (* -1 r)
          (bit-not)
          (bit-and 0xFF)
          (inc)
          (bit-and 0xFF)))))

(defn %8+ [x y]
  (let [x (short x)
        y (short y)]
    (bit-and (+ x y) 0xFF)))

(def %8-
  "Sub numbers and make it a valid word (mod 0xFF)"
  (partial %8 -))

(def %16inc
  "Increment parameter and make it a valid address (mod 0xFFFF)"
  (partial %16+ 1))

(def %16dec
  "Decrement parameter and make it a valid address (mod 0xFFFF)"
  (partial %16+ -1))

(defn dword-at
  ([cpu ^long address]
   {:pre  [(s/cpu? cpu) (s/dword? address)]
    :post [(dword? %)]}
   (cat8 (cpu/word-at cpu (%16+ 1 address))
         (cpu/word-at cpu address))))                                           ;; dword are stored little endian

(defn high-word
  "1 arg version : returns the high word composing the unsigned dword
  2 args version : set the high word of dword to val"
  ([dword]
   {:pre  [(s/dword? dword)]
    :post [(s/word? %)]}
   (let [dword (int dword)]
     (bit-shift-right dword 8)))
  ([dword val]
   {:pre  [(s/dword? dword) (s/word? val)]
    :post [(s/dword? %)]}
   (let [dword (int dword)
         val   (int val)]
     (-> (bit-shift-left val 8)
         (bit-or (bit-and dword 0xFF))))))

(defn low-word
  "1 arg version : returns the low word composing the unsigned dword
  2 args version : set the low word of dword to val"
  ([dword]
   {:pre  [(s/dword? dword)]
    :post [(s/word? %)]}
   (let [dword (int dword)]
     (bit-and dword 0xFF)))
  ([dword val]
   {:pre  [(s/dword? dword) (s/word? val)]
    :post [(s/dword? %)]}
   (let [dword (int dword)
         val   (int val)]
     (bit-or (bit-and dword 0xFF00) val))))

(defn def-dword-register [register]
  (let [register (keyword (name register))]
    (with-meta
      (fn
        ([cpu]
         {:pre  [(s/cpu? cpu)]
          :post [(s/dword? %)]}
         (cpu/dword-register cpu register))
        ([cpu modifier]
         {:pre  [(s/cpu? cpu) (or (fn? modifier) (s/dword? modifier))]
          :post [(s/cpu? %)]}
         (cpu/set-dword-register cpu register modifier)))
      {:type    :operand
       :operand (symbol (name register))})))

(defn def-word-register [select-word dword-register register-name]
  (with-meta
    (fn
      ([cpu]
       (select-word (dword-register cpu)))
      ([cpu val]
       (dword-register cpu (select-word (dword-register cpu) val))))
    {:type    :operand
     :operand register-name}))

(defmethod print-method :operand
  [o ^Writer w]
  (print-method (:operand (meta o)) w))

(def pc (def-dword-register ::s/PC))
(def sp (def-dword-register ::s/SP))
(def af
  (let [normal-dword-register (def-dword-register ::s/AF)]
    (with-meta
      (fn
        ([cpu] (normal-dword-register cpu))
        ([cpu modifier]
         (normal-dword-register cpu
                                (if (fn? modifier)
                                  (comp (partial bit-and 0xFFF0) modifier)
                                  (bit-and 0xFFF0 modifier)))))
      {:type    :operand
       :operand 'af})))
(def bc (def-dword-register ::s/BC))
(def de (def-dword-register ::s/DE))
(def hl (def-dword-register ::s/HL))

(def pure-af (def-dword-register ::s/AF))
(def a (def-word-register high-word pure-af 'a))
(def f (def-word-register low-word pure-af 'f))
(def b (def-word-register high-word bc 'b))
(def c (def-word-register low-word bc 'c))
(def d (def-word-register high-word de 'd))
(def e (def-word-register low-word de 'e))
(def h (def-word-register high-word hl 'h))
(def l (def-word-register low-word hl 'l))

(defn def-flag
  [f-bit-pos]
  (fn
    ([cpu] (bit-test (f cpu) f-bit-pos))
    ([cpu set?]
     {:pre  [(s/cpu? cpu) (boolean? set?)]
      :post [(s/cpu? %)]}
     (if (= (bit-test (f cpu) f-bit-pos) set?)
       cpu
       (f cpu (bit-flip (f cpu) f-bit-pos))))))

(def z? (with-meta (def-flag 7) {:type    :operand
                                 :operand 'z?}))
(def n? (with-meta (def-flag 6) {:type    :operand
                                 :operand 'n?}))
(def h? (with-meta (def-flag 5) {:type    :operand
                                 :operand 'h?}))
(def c? (with-meta (def-flag 4) {:type    :operand
                                 :operand 'c?}))
(def nz? (with-meta (complement z?) {:type    :operand
                                     :operand 'nz?}))
(def nc? (with-meta (complement c?) {:type    :operand
                                     :operand 'nc?}))

(defn set-dword-at [cpu address val]
  {:pre [(dword? address) (dword? val)]}
  (-> (cpu/set-word-at cpu address (low-word val))
      (cpu/set-word-at (inc address) (high-word val))))

(def dword
  (with-meta
    (fn
      ([cpu]
       {:pre  [(s/cpu? cpu)]
        :post [(dword? %)]}
       (cat8 (cpu/word-at cpu (%16+ 2 (pc cpu)))
             (cpu/word-at cpu (%16+ 1 (pc cpu)))))
      ;; todo this arity is never used (remove)
      ([cpu val]
       (cpu/set-word-at cpu (dword cpu) val)))
    {:type    :operand
     :operand 'dword}))
;; synonym to make the code more friendly
(def address dword)

(def word
  (with-meta
    (fn [cpu]
      (cpu/word-at cpu (%16inc (pc cpu))))
    {:type    :operand
     :operand 'word}))

(def sp+n
  (with-meta
    (fn [cpu] (%16+ (sp cpu) (two-complement (word cpu))))
    {:type    :operand
     :operand 'sp+word}))

(def <FF00+n>
  (with-meta
    (fn
      ([cpu]
       (cpu/word-at cpu (+ 0xFF00 (word cpu))))
      ([cpu val]
       (cpu/set-word-at cpu (+ 0xFF00 (word cpu)) val)))
    {:type    :operand
     :operand '<FF00+n>}))

(def <FF00+c>
  (with-meta
    (fn
      ([cpu]
       (cpu/word-at cpu (+ 0xFF00 (c cpu))))
      ([cpu val]
       (cpu/set-word-at cpu (+ 0xFF00 (c cpu)) val)))
    {:type    :operand
     :operand '<FF00+c>}))

(defn register-pointer [dword-register]
  (with-meta
    (fn
      ([cpu]
       (cpu/word-at cpu (dword-register cpu)))
      ([cpu val]
       (cpu/set-word-at cpu (dword-register cpu) val)))
    {:type    :operand
     :operand (symbol (str "<" (:operand (meta dword-register)) ">"))}))

(def <hl> (register-pointer hl))
(def <bc> (register-pointer bc))
(def <de> (register-pointer de))

(def <address>
  (with-meta
    (fn
      ([cpu]
       (cpu/word-at cpu (dword cpu)))
      ([cpu val]
       (cpu/set-word-at cpu (dword cpu) val)))
    {:type    :operand
     :operand '<address>}))


(def always (with-meta (constantly true) {:type    :operand
                                          :operand 'always}))

(defn fetch [cpu]
  {:pre  [(s/cpu? cpu)]
   :post [(not (nil? %))]}
  (cpu/word-at cpu (pc cpu)))

(defprotocol Instr
  (exec [this cpu] "execute this instruction against the cpu")
  (isize [this] "size of this instruction (including operands)")
  (print-assembly [this cpu] "print assembly"))

(defn bool->int [b] (if b 1 0))

(defn rotate-left [^long word]
  {:pre  [(s/word? word)]
   :post [(s/word? %)]}
  (let [highest (bool->int (bit-test word 7))]
    (-> (bit-shift-left word 1)
        (bit-or highest))))

(defn rotate-right [^long word]
  {:pre  [(s/word? word)]
   :post [(s/word? %)]}
  (let [highest (-> (bit-and word 1)
                    (bit-shift-left 7))]
    (-> (bit-shift-right word 1)
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

(defrecord Rlc [word-register]
  Instr
  (exec [this cpu] (rlc cpu word-register (isize this)))
  (isize [_] 1)
  (print-assembly [_ _] (str "rlc " (:operand (meta word-register)))))

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
(defrecord Rrc [word-register]
  Instr
  (exec [this cpu] (rrc cpu word-register (isize this)))
  (isize [_] 1)
  (print-assembly [_ _] (str "rrc " (:operand (meta word-register)))))

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
(defrecord Rl [word-register]
  Instr
  (exec [this cpu] (rl cpu word-register (isize this)))
  (isize [_] 1)
  (print-assembly [_ _] (str "rl" (:operand (meta word-register)))))

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
(defrecord Rr [word-register]
  Instr
  (exec [this cpu] (rr cpu word-register (isize this)))
  (isize [_] 1)
  (print-assembly [_ _] (str "rr " (:operand (meta word-register)))))

(defrecord Sra [word-register]
  Instr
  (exec [this cpu]
    (let [value   (word-register cpu)
          highest (bit-and value 2r10000000)
          result  (-> (bit-shift-right value 1)
                      (bit-or highest))]                                        ;; MSB doesn't change !
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? false)
          (h? false)
          (c? (bit-test value 0))
          (pc (partial %16+ (isize this))))))
  (isize [_] 1)
  (print-assembly [_ _] (str "sra " (:operand (meta word-register)))))

(defrecord Sla [word-register]
  Instr
  (exec [this cpu]
    (let [value  (word-register cpu)
          result (bit-shift-left value 1)]
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? false)
          (h? false)
          (c? (bit-test value 7))
          (pc (partial %16+ (isize this))))))
  (isize [_] 1)
  (print-assembly [_ _] (str "sla " (:operand (meta word-register)))))

(defn low-nibble [word]
  {:pre  [(s/word? word)]
   :post [(s/nibble? %)]}
  (let [word (short word)]
    (bit-and word 0xF)))

(defn high-nibble [word]
  {:pre  [(s/word? word)]
   :post [(s/nibble? %)]}
  (let [word (short word)]
    (bit-shift-right word 4)))

(defn swap [^long word]
  {:pre  [(s/word? word)]
   :post [(s/word? %)]}
  (let [low  (low-nibble word)
        high (high-nibble word)]
    (bit-or (bit-shift-left low 4) high)))

(defrecord Swap [word-register]
  Instr
  (exec [this cpu]
    (let [result (swap (word-register cpu))]
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? false)
          (h? false)
          (c? false)
          (pc (partial %16+ (isize this))))))
  (isize [_] 1)
  (print-assembly [_ _] (str "swap " (:operand (meta word-register)))))

(defrecord Srl [word-register]
  Instr
  (exec [this cpu]
    (let [value  (word-register cpu)
          result (bit-shift-right value 1)]
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? false)
          (h? false)
          (c? (bit-test value 0))
          (pc (partial %16+ (isize this))))))
  (isize [_] 1)
  (print-assembly [_ _] (str "srl " (:operand (meta word-register)))))

(defrecord Bit [position word-register]
  Instr
  (exec [this cpu]
    {:pre [(<= 0 position 7)]}
    (let [result (bit-test (word-register cpu) position)]
      (-> (z? cpu result)
          (n? false)
          (h? true)
          (pc (partial %16+ (isize this))))))
  (isize [_] 1)
  (print-assembly [_ _]
    (str "bit " position " " (:operand (meta word-register)))))

(defrecord Res [position word-register]
  Instr
  (exec [this cpu]
    (-> (word-register cpu #(bit-clear % position))
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _]
    (str "res " position " " (:operand (meta word-register)))))

(defrecord Set [position word-register]
  Instr
  (exec [this cpu]
    (-> (word-register cpu #(bit-set % position))
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _]
    (str "set " position " " (:operand (meta word-register)))))

(def extra-decoder
  [(->Rlc b) (->Rlc c) (->Rlc d) (->Rlc e) (->Rlc h) (->Rlc l) (->Rlc <hl>) (->Rlc a)
   (->Rrc b) (->Rrc c) (->Rrc d) (->Rrc e) (->Rrc h) (->Rrc l) (->Rrc <hl>) (->Rrc a)
   (->Rl b) (->Rl c) (->Rl d) (->Rl e) (->Rl h) (->Rl l) (->Rl <hl>) (->Rl a)
   (->Rr b) (->Rr c) (->Rr d) (->Rr e) (->Rr h) (->Rr l) (->Rr <hl>) (->Rr a)
   (->Sla b) (->Sla c) (->Sla d) (->Sla e) (->Sla h) (->Sla l) (->Sla <hl>) (->Sla a)
   (->Sra b) (->Sra c) (->Sra d) (->Sra e) (->Sra h) (->Sra l) (->Sra <hl>) (->Sra a)
   (->Swap b) (->Swap c) (->Swap d) (->Swap e) (->Swap h) (->Swap l) (->Swap <hl>) (->Swap a)
   (->Srl b) (->Srl c) (->Srl d) (->Srl e) (->Srl h) (->Srl l) (->Srl <hl>) (->Srl a)
   (->Bit 0 b) (->Bit 0 c) (->Bit 0 d) (->Bit 0 e) (->Bit 0 h) (->Bit 0 l) (->Bit 0 <hl>) (->Bit 0 a)
   (->Bit 1 b) (->Bit 1 c) (->Bit 1 d) (->Bit 1 e) (->Bit 1 h) (->Bit 1 l) (->Bit 1 <hl>) (->Bit 1 a)
   (->Bit 2 b) (->Bit 2 c) (->Bit 2 d) (->Bit 2 e) (->Bit 2 h) (->Bit 2 l) (->Bit 2 <hl>) (->Bit 2 a)
   (->Bit 3 b) (->Bit 3 c) (->Bit 3 d) (->Bit 3 e) (->Bit 3 h) (->Bit 3 l) (->Bit 3 <hl>) (->Bit 3 a)
   (->Bit 4 b) (->Bit 4 c) (->Bit 4 d) (->Bit 4 e) (->Bit 4 h) (->Bit 4 l) (->Bit 4 <hl>) (->Bit 4 a)
   (->Bit 5 b) (->Bit 5 c) (->Bit 5 d) (->Bit 5 e) (->Bit 5 h) (->Bit 5 l) (->Bit 5 <hl>) (->Bit 5 a)
   (->Bit 6 b) (->Bit 6 c) (->Bit 6 d) (->Bit 6 e) (->Bit 6 h) (->Bit 6 l) (->Bit 6 <hl>) (->Bit 6 a)
   (->Bit 7 b) (->Bit 7 c) (->Bit 7 d) (->Bit 7 e) (->Bit 7 h) (->Bit 7 l) (->Bit 7 <hl>) (->Bit 7 a)
   (->Res 0 b) (->Res 0 c) (->Res 0 d) (->Res 0 e) (->Res 0 h) (->Res 0 l) (->Res 0 <hl>) (->Res 0 a)
   (->Res 1 b) (->Res 1 c) (->Res 1 d) (->Res 1 e) (->Res 1 h) (->Res 1 l) (->Res 1 <hl>) (->Res 1 a)
   (->Res 2 b) (->Res 2 c) (->Res 2 d) (->Res 2 e) (->Res 2 h) (->Res 2 l) (->Res 2 <hl>) (->Res 2 a)
   (->Res 3 b) (->Res 3 c) (->Res 3 d) (->Res 3 e) (->Res 3 h) (->Res 3 l) (->Res 3 <hl>) (->Res 3 a)
   (->Res 4 b) (->Res 4 c) (->Res 4 d) (->Res 4 e) (->Res 4 h) (->Res 4 l) (->Res 4 <hl>) (->Res 4 a)
   (->Res 5 b) (->Res 5 c) (->Res 5 d) (->Res 5 e) (->Res 5 h) (->Res 5 l) (->Res 5 <hl>) (->Res 5 a)
   (->Res 6 b) (->Res 6 c) (->Res 6 d) (->Res 6 e) (->Res 6 h) (->Res 6 l) (->Res 6 <hl>) (->Res 6 a)
   (->Res 7 b) (->Res 7 c) (->Res 7 d) (->Res 7 e) (->Res 7 h) (->Res 7 l) (->Res 7 <hl>) (->Res 7 a)
   (->Set 0 b) (->Set 0 c) (->Set 0 d) (->Set 0 e) (->Set 0 h) (->Set 0 l) (->Set 0 <hl>) (->Set 0 a)
   (->Set 1 b) (->Set 1 c) (->Set 1 d) (->Set 1 e) (->Set 1 h) (->Set 1 l) (->Set 1 <hl>) (->Set 1 a)
   (->Set 2 b) (->Set 2 c) (->Set 2 d) (->Set 2 e) (->Set 2 h) (->Set 2 l) (->Set 2 <hl>) (->Set 2 a)
   (->Set 3 b) (->Set 3 c) (->Set 3 d) (->Set 3 e) (->Set 3 h) (->Set 3 l) (->Set 3 <hl>) (->Set 3 a)
   (->Set 4 b) (->Set 4 c) (->Set 4 d) (->Set 4 e) (->Set 4 h) (->Set 4 l) (->Set 4 <hl>) (->Set 4 a)
   (->Set 5 b) (->Set 5 c) (->Set 5 d) (->Set 5 e) (->Set 5 h) (->Set 5 l) (->Set 5 <hl>) (->Set 5 a)
   (->Set 6 b) (->Set 6 c) (->Set 6 d) (->Set 6 e) (->Set 6 h) (->Set 6 l) (->Set 6 <hl>) (->Set 6 a)
   (->Set 7 b) (->Set 7 c) (->Set 7 d) (->Set 7 e) (->Set 7 h) (->Set 7 l) (->Set 7 <hl>) (->Set 7 a)])

(defrecord Nop []
  Instr
  (exec [_ cpu]
    (pc cpu %16inc))
  (isize [_] 1)
  (print-assembly [_ _]
    "nop"))

(defrecord Ld [destination source size cycles]
  Instr
  (exec [this cpu]
    (-> (destination cpu (source cpu))
        (pc (partial %16+ (isize this)))))
  (isize [_] size)
  (print-assembly [_ cpu]
    (str "ld "
         (or (:operand (meta destination)) (destination cpu))
         " "
         (or (:operand (meta source)) (source cpu)))))

(defrecord Inc16 [dword-register]
  Instr
  (exec [this cpu]
    (-> (dword-register cpu %16inc)
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] (str "inc " (:operand (meta dword-register)))))

(defrecord Dec16 [dword-register]
  Instr
  (exec [this cpu]
    (-> (dword-register cpu (%16dec (dword-register cpu)))
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _]
    (str "dec " (:operand (meta dword-register)))))

(defrecord Inc [word-register]
  Instr
  (exec [this cpu]
    (let [value  (word-register cpu)
          result (%8+ 1 value)]
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? false)
          (h? (> (inc (low-nibble value)) 0xF))
          (pc (partial %16+ (isize this))))))
  (isize [_] 1)
  (print-assembly [_ _] (str "inc " (:operand (meta word-register)))))

(defrecord Dec [word-register]
  Instr
  (exec [this cpu]
    (let [value  (word-register cpu)
          result (%8 dec value)]
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? true)
          (h? (> (dec (low-nibble value)) 0xF))
          (pc (partial %16+ (isize this))))))
  (isize [_] 1)
  (print-assembly [_ _] (str "dec " (:operand (meta word-register)))))

(defrecord Rlca []
  Instr
  (exec [this cpu]
    (-> (rlc cpu a (isize this))
        (z? false)))
  (isize [_] 1)
  (print-assembly [_ _] "rlca "))

(defrecord AddHl [source]
  Instr
  (exec [this cpu]
    (let [x (hl cpu)
          y (source cpu)]
      (-> (hl cpu (%16+ x y))
          (n? false)
          (h? (> (+ (bit-and x 0x0FFF) (bit-and y 0x0FFF)) 0xFFF))
          (c? (> (+ x y) 0xFFFF))
          (pc (partial %16+ (isize this))))))
  (isize [_] 1)
  (print-assembly [_ _] (str "add hl " (:operand (meta source)))))

(defrecord Rrca []
  Instr
  (exec [_ cpu]
    (-> (rrc cpu a (isize 1))
        (z? false)))
  (isize [_] 1)
  (print-assembly [_ _] "rrca"))

(defrecord Stop []
  Instr
  (exec [this cpu]
    (-> (assoc cpu :mode ::s/stopped)
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] "stop"))

(defrecord Rla []
  Instr
  (exec [this cpu]
    (-> (rl cpu a 1)
        (z? false)
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] "rla"))

(defrecord Jr [cond relative-address]
  Instr
  (exec [this cpu]
    (let [jump            (if (cond cpu)
                            (two-complement (relative-address cpu))
                            0)
          relative-offset (%16+ (isize this) jump)]
      (pc cpu (partial %16+ relative-offset))))
  (isize [_] 2)
  (print-assembly [_ cpu]
    (str "jr " (:operand (meta cond)) " " (relative-address cpu))))

(defrecord Rra []
  Instr
  (exec [this cpu]
    (-> (rr cpu a (isize this))
        (z? false)))
  (isize [_] 1)
  (print-assembly [_ _] "rra"))

(defrecord Ldi [destination source]
  Instr
  (exec [this cpu]
    (-> (destination cpu (source cpu))
        (hl %16inc)
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ cpu]
    (str "ldi "
         (or (:operand (meta destination)) (destination cpu))
         " "
         (or (:operand (meta source)) (source cpu)) " ")))

(defrecord Daa []
  Instr
  (exec [this cpu]
    (-> (af cpu daa)
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] "daa"))

(defrecord Cpl []
  Instr
  (exec [this cpu]
    (-> (a cpu (partial bit-xor 0xFF))                                          ;; todo unsure of implem
        (n? true)
        (h? true)
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] "cpl"))

(defrecord Ldd [destination source]
  Instr
  (exec [this cpu]
    (-> (destination cpu (source cpu))
        (hl %16dec)
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ cpu]
    (str "ldd "
         (or (:operand (meta destination)) (destination cpu))
         " "
         (or (:operand (meta source)) (source cpu)) " ")))

(defrecord Scf []
  Instr
  (exec [this cpu]
    (-> cpu
        (n? false)
        (h? false)
        (c? true)
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] "scf"))

(defrecord Ccf []
  Instr
  (exec [this cpu]
    (-> (n? cpu false)
        (h? false)
        (c? (not (c? cpu)))
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] "ccf"))

(defrecord Halt []
  Instr
  (exec [_ cpu] (assoc cpu :mode ::s/halted))
  (isize [_] 1)
  (print-assembly [_ _] "halt"))

(defn add [cpu x size]
  (let [y      (a cpu)
        result (%8+ x y)]
    (-> (a cpu result)
        (z? (zero? result))
        (n? false)
        (h? (> (+ (low-nibble y) (low-nibble x)) 0xF))
        (c? (> (+ x y) 0xFF))
        (pc (partial %16+ size)))))

(defrecord Add [source size]
  Instr
  (exec [this cpu] (add cpu (source cpu) (isize this)))
  (isize [_] size)
  (print-assembly [_ _] (str "add " (:operand (meta source)))))

(defrecord Adc [source size]
  Instr
  (exec [this cpu]
    (add cpu (%8+ (bool->int (c? cpu)) (source cpu)) (isize this)))
  (isize [_] size)
  (print-assembly [_ _] (str "adc " (:operand (meta source)))))

(defn sub-a [cpu source]
  (let [y (source cpu)
        x (a cpu)]
    (-> (a cpu (%8- x y))
        (z? (= y x))
        (c? (< x y))
        (h? (< (low-nibble x) (low-nibble y)))
        (n? true))))

(defrecord Sub [source size]
  Instr
  (exec [this cpu]
    (-> (sub-a cpu source)
        (pc (partial %16+ (isize this)))))
  (isize [_] size)
  (print-assembly [_ _] (str "sub " (:operand (meta source)))))

(defrecord Sbc [source size]
  Instr
  (exec [this cpu]
    (-> (sub-a cpu (fn [cpu] (%8+ (source cpu) (bool->int (c? cpu)))))
        (pc (partial %16+ (isize this)))))
  (isize [_] size)
  (print-assembly [_ _] (str "sbc " (:operand (meta source)))))

(defrecord And [source size]
  Instr
  (exec [this cpu]
    (let [result (bit-and (source cpu) (a cpu))]
      (-> (a cpu result)
          (z? (= 0 result))
          (n? false)
          (h? true)
          (c? false)
          (pc (partial %16+ (isize this))))))
  (isize [_] size)
  (print-assembly [_ _] (str "and " (:operand (meta source)))))

(defrecord Xor [source size]
  Instr
  (exec [this cpu]
    (let [result (bit-xor (source cpu) (a cpu))]
      (-> (a cpu result)
          (z? (= 0 result))
          (n? false)
          (h? false)
          (c? false)
          (pc (partial %16+ (isize this))))))
  (isize [_] size)
  (print-assembly [_ _] (str "xor " (:operand (meta source)))))

(defrecord Or [source size]
  Instr
  (exec [this cpu]
    (let [value (bit-or (a cpu) (source cpu))]
      (-> (a cpu value)
          (z? (zero? value))
          (n? false)
          (h? false)
          (c? false)
          (pc (partial %16+ (isize this))))))
  (isize [_] size)
  (print-assembly [_ _] (str "or " (:operand (meta source)))))

(defrecord Cp [source size]
  Instr
  (exec [this cpu]
    (-> (sub-a cpu source)
        (a (a cpu))                                                             ;;restore a register (throw away the result)
        (pc (partial %16+ (isize this)))))
  (isize [_] size)
  (print-assembly [_ _] (str "cp " (:operand (meta source)))))

(defn inc-sp [cpu] (sp cpu (partial %16+ 2)))
(defn pop-sp [cpu]
  {:pre  [(s/cpu? cpu)]
   :post [(s/cpu? (second %)) (s/address? (first %))]}
  [(dword-at cpu (sp cpu)) (inc-sp cpu)])

(defrecord Ret [cond]
  Instr
  (exec [this cpu]
    (if (cond cpu)
      (let [[return-address cpu] (pop-sp cpu)]
        (pc cpu return-address))
      (pc cpu (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] (str "ret " (:operand (meta cond)))))

(defrecord Reti []
  Instr
  (exec [_ cpu]
    (let [[return-address cpu] (pop-sp cpu)]
      (pc cpu return-address)))
  (isize [_] 1)
  (print-assembly [_ _] "reti"))

(defrecord Pop [dword-register]
  Instr
  (exec [this cpu]
    (let [[dword cpu] (pop-sp cpu)]
      (-> (dword-register cpu dword)
          (pc (partial %16+ (isize this))))))
  (isize [_] 1)
  (print-assembly [_ _]
    (str "pop " (:operand (meta dword-register)))))

(defn dec-sp [cpu] (sp cpu (partial %16+ -2)))
(defn push-sp [cpu dword]
  {:pre  [(s/cpu? cpu) (s/dword? dword)]
   :post [(s/cpu? %)]}
  (let [cpu (dec-sp cpu)]
    (set-dword-at cpu (sp cpu) dword)))                                         ;; beware : the address should be the decremented sp

(defrecord Push [dword-register]
  Instr
  (exec [this cpu]
    (-> (push-sp cpu (dword-register cpu))
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] (str "push " (:operand (meta dword-register)))))

(defrecord Jp [cond address size]
  Instr
  (exec [_ cpu]
    (if (cond cpu)
      (pc cpu (address cpu))
      (pc cpu (partial + size))))
  (isize [_] size)
  (print-assembly [_ cpu] (str "jp " (hex16 (address cpu)))))

(defn- call [cpu cond address size]
  (let [next-pc (+ size (pc cpu))]
    (if (cond cpu)
      (-> (push-sp cpu next-pc)
          (pc address))
      (pc cpu next-pc))))


(defrecord Call [cond address]
  Instr
  (exec [this cpu]
    (call cpu cond (address cpu) (isize this)))
  (isize [_] 3)
  (print-assembly [_ cpu]
    (str "call " (:operand (meta cond)) " " (address cpu))))

(defrecord Rst [address]
  Instr
  (exec [this cpu]
    (call cpu (constantly true) address (isize this)))
  (isize [_] 1)
  (print-assembly [_ _] (str "rst " address)))

(defrecord Extra []
  Instr
  (exec [this cpu]
    (-> (exec (nth extra-decoder (word cpu)) cpu)                               ;; we don't care if pc is not set correctly when calling exec because extra only needs registers (except pc!) and memory pointer
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)                                                                 ;; given that all extra instructions have a size of 1
  (print-assembly [_ cpu]
    (print-assembly (extra-decoder (word cpu)) cpu)))

(defrecord Breakpoint []
  Instr
  (exec [_ {:keys [x-breakpoints] :as cpu}]
    (let [current-pc (pc cpu)
          [original _] (get x-breakpoints current-pc)]
      (println "processing breakpoint" current-pc original)
      (-> (cpu/set-word-at cpu current-pc original)
          (assoc :mode ::s/break))))
  (isize [_] 1)
  (print-assembly [_ _] "bp"))

(defrecord SkullOfDeath []
  Instr
  (exec [_ _] (throw (Exception. "invalid opcode")))
  (isize [_] 1)
  (print-assembly [_ _] "invalid"))

(defrecord AddSp []
  Instr
  (exec [this cpu]
    (let [x (sp cpu)
          y (word cpu)]
      (-> (sp cpu (%16+ x y))
          (z? false)
          (n? false)
          (h? (> (+ (low-nibble y) (low-nibble x)) 0xF))
          (c? (> (+ (low-word x) y) 0xFF))
          (pc (partial %16+ (isize this))))))
  (isize [_] 2)
  (print-assembly [_ cpu] (str "add sp " (word cpu))))

(defrecord Di []
  Instr
  (exec [this cpu]
    (-> (assoc cpu :interrupt-enabled? false)
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] "di"))

(defrecord Ei []
  Instr
  (exec [this cpu]
    (-> (assoc cpu :interrupt-enabled? true)
        (pc (partial %16+ (isize this)))))
  (isize [_] 1)
  (print-assembly [_ _] "ei"))

(def decoder
  [(->Nop)
   (->Ld bc dword 3 12)
   (->Ld <bc> a 1 8)
   (->Inc16 bc)
   (->Inc b)
   (->Dec b)
   (->Ld b word 2 8)
   (->Rlca)
   (->Ld address sp 3 20)
   (->AddHl bc)
   (->Ld a <bc> 1 8)
   (->Dec16 bc)
   (->Inc c)
   (->Dec c)
   (->Ld c word 2 8)
   (->Rrca)

   ;; 0x10
   (->Stop)
   (->Ld de dword 3 12)
   (->Ld <de> a 1 8)
   (->Inc16 de)
   (->Inc d)
   (->Dec d)
   (->Ld d word 2 8)
   (->Rla)
   (->Jr always word)
   (->AddHl de)
   (->Ld a <de> 1 8)
   (->Dec16 de)
   (->Inc e)
   (->Dec e)
   (->Ld e word 2 8)
   (->Rra)

   ;; 0x20
   (->Jr nz? word)
   (->Ld hl dword 3 12)
   (->Ldi <hl> a)
   (->Inc16 hl)
   (->Inc h)
   (->Dec h)
   (->Ld h word 2 8)
   (->Daa)
   (->Jr z? word)
   (->AddHl hl)
   (->Ldi a <hl>)
   (->Dec16 hl)
   (->Inc l)
   (->Dec l)
   (->Ld l word 2 8)
   (->Cpl)

   ;; 0x30
   (->Jr nc? word)
   (->Ld sp dword 3 12)
   (->Ldd <hl> a)
   (->Inc16 sp)
   (->Inc <hl>)
   (->Dec <hl>)
   (->Ld <hl> word 2 12)
   (->Scf)
   (->Jr c? word)
   (->AddHl bc)
   (->Ldd a <hl>)
   (->Dec16 sp)
   (->Inc a)
   (->Dec a)
   (->Ld a word 2 8)
   (->Ccf)

   ;; 0x40
   (->Ld b b 1 4) (->Ld b c 1 4) (->Ld b d 1 4) (->Ld b e 1 4) (->Ld b h 1 4) (->Ld b l 1 4) (->Ld b <hl> 1 4) (->Ld b a 1 4)
   (->Ld c b 1 4) (->Ld c c 1 4) (->Ld c d 1 4) (->Ld c e 1 4) (->Ld c h 1 4) (->Ld c l 1 4) (->Ld c <hl> 1 4) (->Ld c a 1 4)

   ;; 0x50
   (->Ld d b 1 4) (->Ld d c 1 4) (->Ld d d 1 4) (->Ld d e 1 4) (->Ld d h 1 4) (->Ld d l 1 4) (->Ld d <hl> 1 4) (->Ld d a 1 4)
   (->Ld e b 1 4) (->Ld e c 1 4) (->Ld e d 1 4) (->Ld e e 1 4) (->Ld e h 1 4) (->Ld e l 1 4) (->Ld e <hl> 1 4) (->Ld e a 1 4)

   ;; 0x60
   (->Ld h b 1 4) (->Ld h c 1 4) (->Ld h d 1 4) (->Ld h e 1 4) (->Ld h h 1 4) (->Ld h l 1 4) (->Ld h <hl> 1 4) (->Ld h a 1 4)
   (->Ld l b 1 4) (->Ld l c 1 4) (->Ld l d 1 4) (->Ld l e 1 4) (->Ld l h 1 4) (->Ld l l 1 4) (->Ld l <hl> 1 4) (->Ld l a 1 4)

   ;; 0x70
   (->Ld <hl> b 1 4)
   (->Ld <hl> c 1 4)
   (->Ld <hl> d 1 4)
   (->Ld <hl> e 1 4)
   (->Ld <hl> h 1 4)
   (->Ld <hl> l 1 4)
   (->Halt)
   (->Ld <hl> a 1 4)
   (->Ld a b 1 4) (->Ld a c 1 4) (->Ld a d 1 4) (->Ld a e 1 4) (->Ld a h 1 4) (->Ld a l 1 4) (->Ld a <hl> 1 4) (->Ld a a 1 4)

   ;; 0x80
   (->Add b 1) (->Add c 1) (->Add d 1) (->Add e 1) (->Add h 1) (->Add l 1) (->Add <hl> 1) (->Add a 1)
   (->Adc b 1) (->Adc c 1) (->Adc d 1) (->Adc e 1) (->Adc h 1) (->Adc l 1) (->Adc <hl> 1) (->Adc a 1)

   ;; 0x90
   (->Sub b 1) (->Sub c 1) (->Sub d 1) (->Sub e 1) (->Sub h 1) (->Sub l 1) (->Sub <hl> 1) (->Sub a 1)
   (->Sbc b 1) (->Sbc c 1) (->Sbc d 1) (->Sbc e 1) (->Sbc h 1) (->Sbc l 1) (->Sbc <hl> 1) (->Sbc a 1)

   ;; 0xA0
   (->And b 1) (->And c 1) (->And d 1) (->And e 1) (->And h 1) (->And l 1) (->And <hl> 1) (->And a 1)
   (->Xor b 1) (->Xor c 1) (->Xor d 1) (->Xor e 1) (->Xor h 1) (->Xor l 1) (->Xor <hl> 1) (->Xor a 1)

   ;; 0xB0
   (->Or b 1) (->Or c 1) (->Or d 1) (->Or e 1) (->Or h 1) (->Or l 1) (->Or <hl> 1) (->Or a 1)
   (->Cp b 1) (->Cp c 1) (->Cp d 1) (->Cp e 1) (->Cp h 1) (->Cp l 1) (->Cp <hl> 1) (->Cp a 1)

   ;; 0xC0
   (->Ret nz?)
   (->Pop bc)
   (->Jp nz? address 3)
   (->Jp always address 3)
   (->Call nz? address)
   (->Push bc)
   (->Add word 2)
   (->Rst 0x00)
   (->Ret z?)
   (->Ret always)
   (->Jp z? address 3)
   (->Extra)
   (->Call z? address)
   (->Call always address)
   (->Adc word 2)
   (->Rst 0x08)

   ;; 0xD0
   (->Ret nc?)
   (->Pop de)
   (->Jp nc? address 3)
   (->Breakpoint)
   (->Call nc? address)
   (->Push de)
   (->Sub word 2)
   (->Rst 0x10)
   (->Ret c?)
   (->Reti)
   (->Jp c? address 3)
   (->SkullOfDeath)
   (->Call c? address)
   (->SkullOfDeath)
   (->Sbc word 2)
   (->Rst 0x18)

   ;; 0xE0
   (->Ld <FF00+n> a 2 12)
   (->Pop hl)
   (->Ld <FF00+c> a 2 8)
   (->Breakpoint)
   (->SkullOfDeath)
   (->Push hl)
   (->And word 2)
   (->Rst 0x20)
   (->AddSp)
   (->Jp always hl 1)
   (->Ld <address> a 3 16)
   (->SkullOfDeath)
   (->SkullOfDeath)
   (->SkullOfDeath)
   (->Xor word 2)
   (->Rst 0x28)

   ;; 0xF0
   (->Ld a <FF00+n> 2 12)
   (->Pop af)
   (->Ld a <FF00+c> 2 8)
   (->Di)
   (->SkullOfDeath)
   (->Push af)
   (->Or word 2)
   (->Rst 0x30)
   (->Ld hl sp+n 2 12)
   (->Ld sp hl 1 8)
   (->Ld a <address> 3 12)
   (->Ei)
   (->SkullOfDeath)
   (->SkullOfDeath)
   (->Cp word 2)
   (->Rst 0x38)])

(defn instruction-at-pc [cpu]
  {:pre  [(s/cpu? cpu)]
   :post [(not (nil? %))]}
  (let [opcode (fetch cpu)]
    (nth decoder opcode)))
