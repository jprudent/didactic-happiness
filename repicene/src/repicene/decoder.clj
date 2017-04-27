(ns repicene.decoder
  (:require [repicene.schema :as s :refer [dword? word?]]
            [repicene.bits :refer [two-complement]])
  (:import (java.io Writer)))

(def hex8
  "Transform a word to it's hexadecimal string representation"
  (memoize (partial format "0x%02X")))

(def hex16
  "Transform a dword to it's hexadecimal string representation"
  (memoize (partial format "0x%04X")))

(defn cat8
  "concatenate two words to make a dword"
  [x y]
  {:pre  [(s/word? x) (s/word? y)]
   :post [(s/dword? %)]}
  (bit-or (bit-shift-left x 8) y))

(defn in? [[from to _] address]
  (<= from address to))

(defn %16
  "Address arithmetic should be 0xFFFF modular arithmetic"
  [f & args]
  {:post [(s/dword? %)]}
  (mod (apply f args) 0x10000))                                                 ;; todo bit-and au lieu de mod + éviter apply

(def %16+
  "Add numbers and make it a valid address (mod 0xFFFF)"
  (partial %16 +))

(defn %8
  "Word arithmetic should be 0xFF modular arithmetic"
  [f & args]
  {:post [(s/word? %)]}
  (let [r (apply f args)]
    (if (pos? r)
      (mod r 0x100)
      (-> (* -1 r)
          (bit-not)
          (bit-and 0xFF)
          (inc)
          (mod 0x100)))))

(def %8-
  "Sub numbers and make it a valid word (mod 0xFF)"
  (partial %8 -))

(def %8inc
  "Increment parameter and make it a valid word (mod 0xFF)"
  (partial %8 inc))

(def %8dec
  "Decrement parameter and make it a valid word (mod 0xFF)"
  (partial %8 dec))

(def %16inc
  "Increment parameter and make it a valid address (mod 0xFFFF)"
  (partial %16 inc))

(def %16dec
  "Decrement parameter and make it a valid address (mod 0xFFFF)"
  (partial %16 dec))

(defn lookup-backend [memory address]                                           ;; could be memeoized
  (some (fn [backend]
          (when (in? backend address)
            backend))
        memory))

(defn lookup-backend-index [memory address]                                     ;; could be memoized
  (some (fn [[_ backend :as index-backend]]
          (when (in? backend address)
            index-backend))
        (map vector (range) memory)))

(defn word-at
  ([memory ^long address]
   {:pre  [(dword? address) (s/memory? memory)]
    :post [(word? %)]}
   (let [[from _ backend] (lookup-backend memory address)]
     (let [backend-relative-address (- address from)]
       (nth backend backend-relative-address)))))

(defn dword-at
  ([{:keys [::s/memory]} ^long address]
   {:pre  [(dword? address) (s/memory? memory)]
    :post [(dword? %)]}
   (cat8 (word-at memory (%16+ 1 address)) (word-at memory address))))          ;; dword are stored little endian

(defn high-word
  "1 arg version : returns the high word composing the unsigned dword
  2 args version : set the high word of dword to val"
  ([^long dword]
   {:pre  [(s/dword? dword)]
    :post [(s/word? %)]}
   (bit-shift-right dword 8))
  ([^long dword ^long val]
   {:pre  [(s/dword? dword) (s/word? val)]
    :post [(s/dword? %)]}
   (-> (bit-shift-left val 8)
       (bit-or (bit-and dword 0xFF)))))

(defn low-word
  "1 arg version : returns the low word composing the unsigned dword
  2 args version : set the low word of dword to val"
  ([^long dword]
   {:pre  [(s/dword? dword)]
    :post [(s/word? %)]}
   (bit-and dword 0xFF))
  ([^long dword ^long val]
   {:pre  [(s/dword? dword) (s/word? val)]
    :post [(s/dword? %)]}
   (bit-or (bit-and dword 0xFF00) val)))

(defn def-dword-register [register]
  (with-meta
    (fn
      ([cpu]
       {:pre  [(s/valid? cpu)]
        :post [(s/dword? %)]}
       (get-in cpu [::s/registers register]))
      ([cpu modifier]
       {:pre  [(s/valid? cpu) (or (fn? modifier) (s/dword? modifier))]
        :post [(s/valid? %)]}
       (if (fn? modifier)
         (update-in cpu [::s/registers register] modifier)
         (assoc-in cpu [::s/registers register] modifier))))
    {:type    :operand
     :operand (symbol (name register))}))

(defn def-word-register [high-or-low dword-register register-name]
  (with-meta
    (fn
      ([cpu]
       (high-or-low (dword-register cpu)))
      ([cpu val]
       (dword-register cpu (high-or-low (dword-register cpu) val))))
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

(defn def-flag [pos]
  (fn
    ([cpu] (bit-test (f cpu) pos))
    ([cpu set?]
     {:pre  [(s/valid? cpu) (boolean? set?)]
      :post [(s/valid? %)]}
     (if (= (bit-test (f cpu) pos) set?)
       cpu
       (f cpu (%8 bit-flip (f cpu) pos))))))

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


(defn set-word-at [{:keys [::s/memory w-breakpoints] :as cpu} address val]
  {:pre [(dword? address) (word? val)]}

  (let [[index [from & _]] (lookup-backend-index memory address)
        backend-relative-address (- address from)
        cpu                      (update-in cpu [::s/memory index 2] assoc backend-relative-address val)]
    (if-let [hook (w-breakpoints address)]
      (hook cpu val)
      cpu)))


(defn set-dword-at [cpu address val]
  {:pre [(dword? address) (dword? val)]}
  (-> (set-word-at cpu address (low-word val))
      (set-word-at (inc address) (high-word val))))

(def dword
  (with-meta
    (fn
      ([{:keys [::s/memory] :as cpu}]
       {:pre [(not (nil? cpu)) (not (nil? memory))]}
       (cat8 (word-at memory (+ 2 (pc cpu)))
             (word-at memory (+ 1 (pc cpu)))))
      ([cpu val]
       (set-word-at cpu (dword cpu) val)))
    {:type    :operand
     :operand 'dword}))
;; synonym to make the code more friendly
(def address dword)

(def word
  (with-meta
    (fn [{:keys [::s/memory] :as cpu}] (word-at memory (%16 inc (pc cpu))))
    {:type    :operand
     :operand 'word}))

(def sp+n
  (with-meta
    (fn [cpu] (%16 + (sp cpu) (two-complement (word cpu))))
    {:type    :operand
     :operand 'sp+word}))

(def <FF00+n>
  (with-meta
    (fn
      ([{:keys [::s/memory] :as cpu}]
       (word-at memory (+ 0xFF00 (word cpu))))
      ([cpu val]
       (set-word-at cpu (+ 0xFF00 (word cpu)) val)))
    {:type    :operand
     :operand '<FF00+n>}))

(def <FF00+c>
  (with-meta
    (fn
      ([{:keys [::s/memory] :as cpu}]
       (word-at memory (+ 0xFF00 (c cpu))))
      ([cpu val]
       (set-word-at cpu (+ 0xFF00 (c cpu)) val)))
    {:type    :operand
     :operand '<FF00+c>}))

(defn register-pointer [dword-register]
  (with-meta
    (fn
      ([{:keys [::s/memory] :as cpu}]
       (word-at memory (dword-register cpu)))
      ([cpu val]
       (set-word-at cpu (dword-register cpu) val)))
    {:type    :operand
     :operand (symbol (str "<" (:operand (meta dword-register)) ">"))}))

(def <hl> (register-pointer hl))
(def <bc> (register-pointer bc))
(def <de> (register-pointer de))

(def <address>
  (with-meta
    (fn
      ([{:keys [::s/memory] :as cpu}]
       (word-at memory (dword cpu)))
      ([cpu val]
       (set-word-at cpu (dword cpu) val)))
    {:type    :operand
     :operand '<address>}))


(def always (with-meta (constantly true) {:type    :operand
                                          :operand 'always}))

(def hex-dword (comp hex16 dword))
(def hex-word (comp hex8 word))

(defn fetch [{:keys [::s/memory] :as cpu}]
  {:pre  [(s/valid? cpu)]
   :post [(not (nil? %))]}
  #_(println "fetch " (hex8 (word-at memory (pc cpu))))
  (word-at memory (pc cpu)))

(defrecord instruction [asm cycles size to-string])
(def unknown (->instruction [:wtf] 0 1 (constantly "???")))

(defprotocol Instr
  (exec [this cpu] "execute this instruction against the cpu")
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
  (exec [{:keys [word-register]} cpu]
    (rlc cpu word-register 1))
  (print-assembly [{:keys [word-register]} _]
    (str "rlc " (:operand (meta word-register)))))

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
  (exec [{:keys [word-register]} cpu]
    (rrc cpu word-register 1))
  (print-assembly [{:keys [word-register]} _]
    (str "rrc " (:operand (meta word-register)))))

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
  (exec [{:keys [word-register]} cpu]
    (rl cpu word-register 1))
  (print-assembly [{:keys [word-register]} _]
    (str "rl" (:operand (meta word-register)))))

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
  (exec [{:keys [word-register]} cpu]
    (rr cpu word-register 1))
  (print-assembly [{:keys [word-register]} _]
    (str "rr " (:operand (meta word-register)))))

(defrecord Sra [word-register]
  Instr
  (exec [{:keys [word-register]} cpu]
    (let [value   (word-register cpu)
          highest (bit-and value 2r10000000)
          result  (-> (bit-shift-right value 1)
                      (bit-or highest))]                                        ;; MSB doesn't change !
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? false)
          (h? false)
          (c? (bit-test value 0))
          (pc (partial %16+ 1)))))
  (print-assembly [{:keys [word-register]} _]
    (str "sra " (:operand (meta word-register)))))

(defrecord Sla [word-register]
  Instr
  (exec [{:keys [word-register]} cpu]
    (let [value  (word-register cpu)
          result (bit-shift-left value 1)]
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? false)
          (h? false)
          (c? (bit-test value 7))
          (pc (partial %16+ 1)))))
  (print-assembly [{:keys [word-register]} _]
    (str "sla " (:operand (meta word-register)))))

(defn low-nibble [word]
  {:pre  [(s/word? word)]
   :post [(s/nibble? %)]}
  (bit-and word 0xF))

(defn high-nibble [word]
  {:pre  [(s/word? word)]
   :post [(s/nibble? %)]}
  (bit-shift-right word 4))

(defn swap [^long word]
  {:pre  [(s/word? word)]
   :post [(s/word? %)]}
  (let [low  (low-nibble word)
        high (high-nibble word)]
    (bit-or (bit-shift-left low 4) high)))

(defrecord Swap [word-register]
  Instr
  (exec [{:keys [word-register]} cpu]
    (let [result (swap (word-register cpu))]
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? false)
          (h? false)
          (c? false)
          (pc (partial %16+ 1)))))
  (print-assembly [{:keys [word-register]} _]
    (str "swap " (:operand (meta word-register)))))

(defrecord Srl [word-register]
  Instr
  (exec [{:keys [word-register]} cpu]
    (let [value  (word-register cpu)
          result (bit-shift-right value 1)]
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? false)
          (h? false)
          (c? (bit-test value 0))
          (pc (partial %16+ 1)))))
  (print-assembly [{:keys [word-register]} _]
    (str "srl " (:operand (meta word-register)))))

(defrecord Bit [position word-register]
  Instr
  (exec [{:keys [position word-register]} cpu]
    {:pre [(<= 0 position 7)]}
    (-> (z? (bit-test (word-register cpu) position))
        (n? false)
        (h? true)
        (pc (partial %16+ 1))))
  (print-assembly [{:keys [position word-register]} _]
    (str "bit " position " " (:operand (meta word-register)))))

(defrecord Res [position word-register]
  Instr
  (exec [{:keys [position word-register]} cpu]
    (-> (word-register cpu #(bit-clear % position))
        (pc (partial %16+ 1))))
  (print-assembly [{:keys [position word-register]} _]
    (str "res " position " " (:operand (meta word-register)))))

(defrecord Set [position word-register]
  Instr
  (exec [{:keys [position word-register]} cpu]
    (-> (word-register cpu #(bit-set % position))
        (pc (partial %16+ 1))))
  (print-assembly [{:keys [position word-register]} _]
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
    (pc cpu inc))
  (print-assembly [_ _]
    "nop"))

(defrecord Ld [source destination size cycles]
  Instr
  (exec [{:keys [destination source size]} cpu]
    {:post [(is (= (source cpu) (destination (pc % (pc cpu)))))]}
    (-> (destination cpu (source cpu))
        (pc (partial %16+ size))))
  (print-assembly [{:keys [destination source]} cpu]
    (str "ld "
         (or (:operand (meta destination)) (destination cpu))
         " "
         (or (:operand (meta source)) (source cpu)) " ")))

(defrecord Inc16 [dword-register]
  Instr
  (exec [{:keys [dword-register]} cpu]
    {:post [(= (%16+ 1 (dword-register cpu)) (dword-register %))
            (= (pc %) (%16+ 1 (pc cpu)))]}
    (-> (dword-register cpu %16inc)
        (pc (partial %16+ 1))))
  (print-assembly [{:keys [dword-register]} _]
    (str "inc " (:operand (meta dword-register)))))

(defrecord Dec16 [dword-register]
  Instr
  (exec [{:keys [dword-register]} cpu]
    (-> (dword-register cpu (%16 dec (dword-register cpu)))
        (pc (partial %16+ 1))))
  (print-assembly [{:keys [dword-register]} _]
    (str "dec " (:operand (meta dword-register)))))

(defrecord Inc [word-register]
  Instr
  (exec [{:keys [word-register]} cpu]
    (let [value  (word-register cpu)
          result (%8 inc value)]
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? false)
          (h? (> (inc (low-nibble value)) 0xF))
          (pc (partial %16+ 1)))))
  (print-assembly [{:keys [dword-register]} _]
    (str "inc " (:operand (meta dword-register)))))

(defrecord Dec [word-register]
  Instr
  (exec [{:keys [word-register]} cpu]
    (let [value  (word-register cpu)
          result (%8 dec value)]
      (-> (word-register cpu result)
          (z? (zero? result))
          (n? true)
          (h? (> (dec (low-nibble value)) 0xF))
          (pc (partial %16+ 1)))))
  (print-assembly [{:keys [dword-register]} _]
    (str "dec " (:operand (meta dword-register)))))

(defrecord Rlca []
  Instr
  (exec [_ cpu]
    (-> (rlc cpu a 1)
        (z? false)))
  (print-assembly [_ _]
    "rlca "))

(defrecord AddHl [source]
  Instr
  (exec [{:keys [source]} cpu]
    {:pre  [(s/valid? cpu)]
     :post [(s/valid? %)]}
    (let [x (hl cpu)
          y (source cpu)]
      (-> (hl cpu (%16+ x y))
          (n? false)
          (h? (> (+ (bit-and x 0x0FFF) (bit-and y 0x0FFF)) 0xFFF))
          (c? (> (+ x y) 0xFFFF))
          (pc (partial %16+ 1)))))
  (print-assembly [{:keys [source]} _]
    (str "add hl " (:operand (meta source)))))

(defrecord Rrca []
  Instr
  (exec [_ cpu]
    (-> (rrc cpu a 1)
        (z? false)))
  (print-assembly [_ _] "rrca"))

(defrecord Stop []
  Instr
  (exec [_ cpu]
    {:post [(s/valid? %) (= ::s/stopped (::s/mode %))]}
    (-> (assoc cpu ::s/mode ::s/stopped)
        (pc (partial %16+ 1))))
  (print-assembly [_ _] "stop"))

(defrecord Rla []
  Instr
  (exec [cpu]
    (-> (rl cpu a 1)
        (z? false)))
  (print-assembly [_ _] "rla"))

(defrecord Jr [cond relative-address]
  Instr
  (exec [{:keys [cond relative-address]} cpu]
    {:pre  [(s/valid? cpu) (s/word? (relative-address cpu))]
     :post [(s/valid? %)]}
    (let [jump (if (cond cpu) (two-complement (relative-address cpu)) 0)]
      (pc cpu (partial %16+ 2 jump))))
  (print-assembly [{:keys [cond relative-address]} cpu]
    (str "jr " (:operand (meta cond)) " " (relative-address cpu))))

(defrecord Rra []
  Instr
  (exec [_ cpu]
    (-> (rr cpu a 1)
        (z? false)))
  (print-assembly [_ _] "rra"))

(defrecord Ldi [destination source]
  Instr
  (exec [{:keys [destination source]} cpu]
    {:post [(= (%16inc (hl cpu)) (hl %))
            (= (pc %) (%16+ 1 (pc cpu)))]}
    (-> (destination cpu (source cpu))
        (hl %16inc)
        (pc (partial %16+ 1)))))

(defrecord Daa []
  Instr
  (exec [_ cpu]
    (-> (af cpu daa)
        (pc (partial %16+ 1))))
  (print-assembly [_ _] "daa"))

(defrecord Cpl []
  Instr
  (exec [_ cpu]
    (-> (a cpu (partial bit-xor 0xFF))                                          ;; todo unsure of implem
        (n? true)
        (h? true)
        (pc (partial %16+ 1))))
  (print-assembly [_ _] "cpl"))

(defrecord Ldd [destination source]
  Instr
  (exec [{:keys [destination source]} cpu]
    {:pre  [(s/valid? cpu)]
     :post [(s/valid? %)
            (= (%16dec (hl cpu)) (hl %))
            (= (pc %) (%16+ 1 (pc cpu)))]}
    (-> (destination cpu (source cpu))
        (hl %16dec)
        (pc (partial %16+ 1))))
  (print-assembly [{:keys [destination source]} cpu]
    (str "ldd "
         (or (:operand (meta destination)) (destination cpu))
         " "
         (or (:operand (meta source)) (source cpu)) " ")))

(defrecord Scf []
  Instr
  (exec [_ cpu]
    (-> (n? false)
        (h? false)
        (c? true)
        (pc (partial %16+ 1))))
  (print-assembly [_ _] "scf"))

(defrecord Ccf []
  Instr
  (exec [_ cpu]
    (-> (n? cpu false)
        (h? false)
        (c? (not (c? cpu)))
        (pc (partial %16+ 1))))
  (print-assembly [_ _] "ccf"))

(defrecord Halt []
  Instr
  (exec [_ cpu]
    (throw (Exception. "unimplemented")))
  (print-assembly [_ _] "halt"))

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
   (->Ld b b 1 4)
   (->Ld b c 1 4)
   (->Ld b d 1 4)
   (->Ld b e 1 4)
   (->Ld b h 1 4)
   (->Ld b l 1 4)
   (->Ld b <hl> 1 4)
   (->Ld b a 1 4)
   (->Ld c b 1 4)
   (->Ld c c 1 4)
   (->Ld c d 1 4)
   (->Ld c e 1 4)
   (->Ld c h 1 4)
   (->Ld c l 1 4)
   (->Ld c <hl> 1 4)
   (->Ld c a 1 4)
   (->Ld d b 1 4)
   (->Ld d c 1 4)
   (->Ld d d 1 4)
   (->Ld d e 1 4)
   (->Ld d h 1 4)
   (->Ld d l 1 4)
   (->Ld d <hl> 1 4)
   (->Ld d a 1 4)
   (->Ld e b 1 4)
   (->Ld e c 1 4)
   (->Ld e d 1 4)
   (->Ld e e 1 4)
   (->Ld e h 1 4)
   (->Ld e l 1 4)
   (->Ld e <hl> 1 4)
   (->Ld e a 1 4)
   (->Ld h b 1 4)
   (->Ld h c 1 4)
   (->Ld h d 1 4)
   (->Ld h e 1 4)
   (->Ld h h 1 4)
   (->Ld h l 1 4)
   (->Ld h <hl> 1 4)
   (->Ld h a 1 4)
   (->Ld l b 1 4)
   (->Ld l c 1 4)
   (->Ld l d 1 4)
   (->Ld l e 1 4)
   (->Ld l h 1 4)
   (->Ld l l 1 4)
   (->Ld l <hl> 1 4)
   (->Ld l a 1 4)
   (->Ld <hl> b 1 4)
   (->Ld <hl> c 1 4)
   (->Ld <hl> d 1 4)
   (->Ld <hl> e 1 4)
   (->Ld <hl> h 1 4)
   (->Ld <hl> l 1 4)
   (->Halt)
   (->Ld <hl> a 1 4)
   (->Ld a b 1 4)
   (->Ld a c 1 4)
   (->Ld a d 1 4)
   (->Ld a e 1 4)
   (->Ld a h 1 4)
   (->Ld a l 1 4)
   (->Ld a <hl> 1 4)
   (->Ld a a 1 4)
   0x80 (->instruction [:add b] 4 1 (constantly "add b"))
   0x81 (->instruction [:add c] 4 1 (constantly "add c"))
   0x82 (->instruction [:add d] 4 1 (constantly "add d"))
   0x83 (->instruction [:add e] 4 1 (constantly "add e"))
   0x84 (->instruction [:add h] 4 1 (constantly "add h"))
   0x85 (->instruction [:add l] 4 1 (constantly "add l"))
   0x86 (->instruction [:add <hl>] 4 1 (constantly "add [hl]"))
   0x87 (->instruction [:add a] 4 1 (constantly "add a"))
   0x88 (->instruction [:adc b] 4 1 (constantly "adc b"))
   0x89 (->instruction [:adc c] 4 1 (constantly "adc c"))
   0x8A (->instruction [:adc d] 4 1 (constantly "adc d"))
   0x8B (->instruction [:adc e] 4 1 (constantly "adc e"))
   0x8C (->instruction [:adc h] 4 1 (constantly "adc h"))
   0x8D (->instruction [:adc l] 4 1 (constantly "adc l"))
   0x8E (->instruction [:adc <hl>] 4 1 (constantly "adc <hl>"))
   0x8F (->instruction [:adc a] 4 1 (constantly "adc a"))
   0x90 (->instruction [:sub b] 4 1 (constantly "sub b"))
   0x91 (->instruction [:sub c] 4 1 (constantly "sub c"))
   0x92 (->instruction [:sub d] 4 1 (constantly "sub d"))
   0x93 (->instruction [:sub e] 4 1 (constantly "sub e"))
   0x94 (->instruction [:sub h] 4 1 (constantly "sub h"))
   0x95 (->instruction [:sub l] 4 1 (constantly "sub l"))
   0x96 (->instruction [:sub <hl>] 8 1 (constantly "sub [hl]"))
   0x97 (->instruction [:sub a] 4 1 (constantly "sub a"))
   0x98 (->instruction [:sbc b] 4 1 (constantly "sbc b"))
   0x99 (->instruction [:sbc c] 4 1 (constantly "sbc c"))
   0x9A (->instruction [:sbc d] 4 1 (constantly "sbc d"))
   0x9B (->instruction [:sbc e] 4 1 (constantly "sbc e"))
   0x9C (->instruction [:sbc h] 4 1 (constantly "sbc h"))
   0x9D (->instruction [:sbc l] 4 1 (constantly "sbc l"))
   0x9E (->instruction [:sbc <hl>] 4 1 (constantly "sbc <hl>"))
   0x9F (->instruction [:sbc a] 4 1 (constantly "sbc a"))
   0xA0 (->instruction [:and b] 4 1 (constantly "and b"))
   0xA1 (->instruction [:and c] 4 1 (constantly "and c"))
   0xA2 (->instruction [:and d] 4 1 (constantly "and d"))
   0xA3 (->instruction [:and e] 4 1 (constantly "and e"))
   0xA4 (->instruction [:and h] 4 1 (constantly "and h"))
   0xA5 (->instruction [:and l] 4 1 (constantly "and l"))
   0xA6 (->instruction [:and <hl>] 8 1 (constantly "and [hl]"))
   0xA7 (->instruction [:and a] 4 1 (constantly "and a"))
   0xA8 (->instruction [:xor b] 4 1 (constantly "xor b"))
   0xA9 (->instruction [:xor c] 4 1 (constantly "xor c"))
   0xAA (->instruction [:xor d] 4 1 (constantly "xor d"))
   0xAB (->instruction [:xor e] 4 1 (constantly "xor e"))
   0xAC (->instruction [:xor h] 4 1 (constantly "xor h"))
   0xAD (->instruction [:xor l] 4 1 (constantly "xor l"))
   0xAE (->instruction [:xor <hl>] 8 1 (constantly "xor <hl>"))
   0xAF (->instruction [:xor a] 4 1 (constantly "xor a"))
   0xB0 (->instruction [:or b] 4 1 (constantly "or b"))
   0xB1 (->instruction [:or c] 4 1 (constantly "or c"))
   0xB2 (->instruction [:or d] 4 1 (constantly "or d"))
   0xB3 (->instruction [:or e] 4 1 (constantly "or e"))
   0xB4 (->instruction [:or h] 4 1 (constantly "or h"))
   0xB5 (->instruction [:or l] 4 1 (constantly "or l"))
   0xB6 (->instruction [:or <hl>] 4 1 (constantly "or [hl]"))
   0xB7 (->instruction [:or a] 4 1 (constantly "or a"))
   0xB8 (->instruction [:cp b] 4 1 (constantly "cp b"))
   0xB9 (->instruction [:cp c] 4 1 (constantly "cp c"))
   0xBA (->instruction [:cp d] 4 1 (constantly "cp d"))
   0xBB (->instruction [:cp e] 4 1 (constantly "cp e"))
   0xBC (->instruction [:cp h] 4 1 (constantly "cp h"))
   0xBD (->instruction [:cp l] 4 1 (constantly "cp l"))
   0xBE (->instruction [:cp <hl>] 8 1 (constantly "cp [hl]"))
   0xBF (->instruction [:cp a] 4 1 (constantly "cp a"))
   0xC0 (->instruction [:ret nz?] [20 8] 3 (constantly "ret nz"))
   0xC1 (->instruction [:pop bc] 12 1 (constantly "pop bc"))
   0xC2 (->instruction [:jp nz? address] [16 12] 3 #(str "jp nz " (hex-dword %)))
   0xC3 (->instruction [:jp always address] 8 3 #(str "jp " (hex-dword %)))
   0xC4 (->instruction [:call nz? address] 24 3 #(str "call nz " (hex-dword %)))
   0xC5 (->instruction [:push bc] 16 1 (constantly "push bc"))
   0xC6 (->instruction [:add word] 8 2 #(str "add " (hex-word %)))
   0xC7 (->instruction [:rst 0x00] 16 1 (constantly "rst 00"))
   0xC8 (->instruction [:ret z?] [20 8] 1 (constantly "ret z"))
   0xC9 (->instruction [:ret always] 16 1 (constantly "ret"))
   0xCA (->instruction [:jp z? address] [16 12] 3 #(str "jp z " (hex-dword %)))
   0xCB (->instruction [:extra word] 4 2 (fn [cpu]
                                           ((:to-string (extra-decoder (word cpu))) cpu))) ;; Size is 1 because extra instructions have size 1 too
   0xCC (->instruction [:call z? address] 24 3 #(str "call z " (hex-dword %)))
   0xCD (->instruction [:call always address] 24 3 #(str "call " (hex-dword %)))
   0xCE (->instruction [:adc word] 8 2 #(str "adc " (hex-word %)))
   0xCF (->instruction [:rst 0x08] 16 1 (constantly "rst 08"))
   0xD0 (->instruction [:ret nc?] [20 8] 1 (constantly "ret nc"))
   0xD1 (->instruction [:pop de] 12 1 (constantly "pop de"))
   0xD2 (->instruction [:jp nc? address] [16 12] 3 #(str "jp nc " (hex-dword %)))
   0xD3 (->instruction [:breakpoint] 0 1 (constantly "bp"))
   0xD4 (->instruction [:call nc? address] 24 3 #(str "call nc " (hex-dword %)))
   0xD5 (->instruction [:push de] 16 1 (constantly "push de"))
   0xD6 (->instruction [:sub word] 8 2 #(str "sub " (hex-word %)))
   0xD7 (->instruction [:rst 0x10] 16 1 (constantly "rst 10"))
   0xD8 (->instruction [:ret c?] [20 8] 1 (constantly "ret c"))
   0xD9 (->instruction [:ret] 16 1 (constantly "reti"))
   0xDA (->instruction [:jp c? address] [16 12] 3 #(str "jp c " (hex-dword %)))
   0xDB (->instruction [:invalid] 0 1 (constantly "invalid"))
   0xDC (->instruction [:call c? address] 24 3 #(str "call " (hex-dword %)))
   0xDD (->instruction [:invalid] 0 1 (constantly "invalid"))
   0xDE (->instruction [:sbc word] 8 2 #(str "sbc " (hex-word %)))
   0xDF (->instruction [:rst 0x18] 16 1 (constantly "rst 18"))
   0xE0 (->instruction [:ld <FF00+n> a] 12 2 #(str "ldh [FF00+" (hex-word %) "],a"))
   0xE1 (->instruction [:pop hl] 12 1 (constantly "pop hl"))
   0xE2 (->instruction [:ld <FF00+c> a] 8 2 (constantly "ld [c],a"))
   0xE3 (->instruction [:once-breakpoint] 0 1 (constantly "once bp"))
   0xE4 (->instruction [:invalid] 0 1 (constantly "invalid"))
   0xE5 (->instruction [:push hl] 16 1 (constantly "push hl"))
   0xE6 (->instruction [:and word] 4 2 #(str "and " (hex-word %)))
   0xE7 (->instruction [:rst 0x20] 16 1 (constantly "rst 20"))
   0xE8 (->instruction [:add-sp word] 16 2 #(str "add sp," (hex-word %)))
   0xE9 (->instruction [:jp always hl] 4 1 (constantly "jp hl"))
   0xEA (->instruction [:ld <address> a] 16 3 #(str "ld [" (hex-dword %) "],a"))
   0xEB (->instruction [:invalid] 0 1 (constantly "invalid"))
   0xEC (->instruction [:invalid] 0 1 (constantly "invalid"))
   0xED (->instruction [:invalid] 0 1 (constantly "invalid"))
   0xEE (->instruction [:xor word] 8 2 #(str "xor " (hex-word %)))
   0xEF (->instruction [:rst 0x28] 16 1 (constantly "rst 28"))
   0xF0 (->instruction [:ld a <FF00+n>] 12 2 #(str "ldh a,[FF00+" (hex-word %) "]"))
   0xF1 (->instruction [:pop af] 12 1 (constantly "pop af"))
   0xF2 (->instruction [:ld a, <FF00+c>] 8 2 (constantly "ld a,[c]"))
   0xF3 (->instruction [:di] 4 1 (constantly "di"))
   0xF4 (->instruction [:invalid] 0 1 (constantly "invalid"))
   0xF5 (->instruction [:push af] 16 1 (constantly "push af"))
   0xF6 (->instruction [:or word] 8 2 #(str "or " (hex-word %)))
   0xF7 (->instruction [:rst 0x30] 16 1 (constantly "rst 30"))
   0xF8 (->instruction [:ld hl sp+n] 12 2 #(str "ld hl,sp+" (hex-word %)))
   0xF9 (->instruction [:ld sp hl] 8 1 (constantly "ld sp,hl"))
   0xFA (->instruction [:ld a <address>] 12 3 #(str "ldh a,[" (hex-dword %) "]"))
   0xFB (->instruction [:ei] 4 1 (constantly "ei"))
   0xFC (->instruction [:invalid] 0 1 (constantly "invalid"))
   0xFD (->instruction [:invalid] 0 1 (constantly "invalid"))
   0xFE (->instruction [:cp word] 8 2 #(str "cp " (hex-word %)))
   0xFF (->instruction [:rst 0x38] 16 1 (constantly "rst 38"))])

(defn instruction-at-pc [cpu]
  {:pre  [(s/valid? cpu)]
   :post [(not (nil? %))]}
  (get decoder (fetch cpu)))

#_(defn mkop [op start]
    (map
      (fn [i reg]
        (str (hex8 i) " (->instruction [:" op " " reg "] 4 1 (constantly \"" op " " reg "\"))"))
      (range start (+ start 8)) ["b" "c" "d" "e" "h" "l" "<hl>" "a"]))

#_(defn mkop2 [op n start]
    (map
      (fn [i reg]
        (str (hex8 i) " (->instruction [:" op " " n " " reg "] " (if (= reg "<hl>") 8 4) " 1 (constantly \"" op " " n "," (if (= reg "<hl>") "[hl]" reg) "\"))"))
      (range start (+ start 8)) ["b" "c" "d" "e" "h" "l" "<hl>" "a"]))