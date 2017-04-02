(ns repicene.decoder
  (:require [repicene.schema :as s :refer [dword? word?]]))

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
  (mod (apply f args) 0x10000))

(def %16+
  "Add numbers and make it a valid address (mod 0xFFFF)"
  (partial %16 +))

(defn %8
  "Word arithmetic should be 0xFF modular arithmetic"
  [f & args]
  {:post [(s/word? %)]}
  (mod (apply f args) 0xFF))

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
  ([memory address]
   {:pre  [(dword? address) (s/memory? memory)]
    :post [(word? %)]}
   (let [[from _ backend] (lookup-backend memory address)]
     (let [backend-relative-address (- address from)]
       (nth backend backend-relative-address)))))

(defn dword-at
  ([{:keys [::s/memory]} address]
   {:pre  [(dword? address) (s/memory? memory)]
    :post [(dword? %)]}
   (cat8 (word-at memory (%16+ 1 address)) (word-at memory address))))          ;; dword are stored little endian

(defn high-word
  "1 arg version : returns the high word composing the unsigned dword
  2 args version : set the high word of dword to val"
  ([dword]
   {:pre  [(s/dword? dword)]
    :post [(s/word? %)]}
   (bit-shift-right dword 8))
  ([dword val]
   {:pre  [(s/dword? dword) (s/word? val)]
    :post [(s/dword? %)]}
   (-> (bit-shift-left val 8)
       (bit-or (bit-and dword 0xFF)))))

(defn low-word
  "1 arg version : returns the low word composing the unsigned dword
  2 args version : set the low word of dword to val"
  ([dword]
   {:pre  [(s/dword? dword)]
    :post [(s/word? %)]}
   (bit-and dword 0xFF))
  ([dword val]
   {:pre  [(s/dword? dword) (s/word? val)]
    :post [(s/dword? %)]}
   (bit-or (bit-and dword 0xFF00) val)))

(defn def-dword-register [register]
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
       (assoc-in cpu [::s/registers register] modifier)))))

(defn def-word-register [high-or-low dword-register]
  (fn
    ([cpu]
     (high-or-low (dword-register cpu)))
    ([cpu val]
     (dword-register cpu (high-or-low (dword-register cpu) val)))))

(def pc (def-dword-register ::s/PC))
(def sp (def-dword-register ::s/SP))
(def af (def-dword-register ::s/AF))
(def bc (def-dword-register ::s/BC))
(def de (def-dword-register ::s/DE))
(def hl (def-dword-register ::s/HL))

(def a (def-word-register high-word af))
(def f (def-word-register low-word af))
(def b (def-word-register high-word bc))
(def c (def-word-register low-word bc))
(def d (def-word-register high-word de))
(def e (def-word-register low-word de))
(def h (def-word-register high-word hl))
(def l (def-word-register low-word hl))

(defn def-flag [pos]
  (fn
    ([cpu] (bit-test (f cpu) pos))
    ([cpu set?]
     {:pre  [(s/valid? cpu) (boolean? set?)]
      :post [(s/valid? %)]}
     (if (= (bit-test (f cpu) pos) set?)
       cpu
       (f cpu (%8 bit-flip (f cpu) pos))))))

(def z? (def-flag 7))
(def n? (def-flag 6))
(def h? (def-flag 5))
(def c? (def-flag 4))
(def nz? (complement z?))
(def nc? (complement c?))

(defn set-word-at [{:keys [::s/memory] :as cpu} address val]
  {:pre [(dword? address) (word? val)]}
  #_(println "word-at " (hex16 address))
  (let [[index [from & _]] (lookup-backend-index memory address)
        backend-relative-address (- address from)]
    (update-in cpu [::s/memory index 2] assoc backend-relative-address val)))

(defn set-dword-at [cpu address val]
  {:pre [(dword? address) (dword? val)]}
  (-> (set-word-at cpu address (low-word val))
      (set-word-at (inc address) (high-word val))))

(defn dword
  ([{:keys [::s/memory] :as cpu}]
   {:pre [(not (nil? cpu)) (not (nil? memory))]}
   (cat8 (word-at memory (+ 2 (pc cpu)))
         (word-at memory (+ 1 (pc cpu)))))
  ([cpu val]
   (set-word-at cpu (dword cpu) val)))
;; synonym to make the code more friendly
(def address dword)

(defn word
  [{:keys [::s/memory] :as cpu}]
  (word-at memory (inc (pc cpu))))

(defn <FF00+n>
  ([{:keys [::s/memory] :as cpu}]
   (word-at memory (+ 0xFF00 (word cpu))))
  ([cpu val]
   (set-word-at cpu (+ 0xFF00 (word cpu)) val)))

(defn register-pointer [dword-register]
  (fn
    ([{:keys [::s/memory] :as cpu}]
     (word-at memory (dword-register cpu)))
    ([cpu val]
     (set-word-at cpu (dword-register cpu) val))))

(def <hl> (register-pointer hl))
(def <bc> (register-pointer bc))
(def <de> (register-pointer de))

(defn <address>
  ([{:keys [::s/memory] :as cpu}]
   (word-at memory (dword cpu)))
  ([cpu val]
   (set-word-at cpu (dword cpu) val)))


(def always (constantly true))

(def hex-dword (comp hex16 dword))
(def hex-word (comp hex8 word))

(defn fetch [{:keys [::s/memory] :as cpu}]
  {:pre  [(s/valid? cpu)]
   :post [(not (nil? %))]}
  (println "fetch " (hex8 (word-at memory (pc cpu))))
  (word-at memory (pc cpu)))

(defrecord instruction [asm cycles size to-string])
(def unknown (->instruction [:wtf] 0 1 (constantly "???")))

(def extra-decoder
  {0x00 (->instruction [:rlc b] 4 1 (constantly "rlc b"))
   0x01 (->instruction [:rlc c] 4 1 (constantly "rlc c"))
   0x02 (->instruction [:rlc d] 4 1 (constantly "rlc d"))
   0x03 (->instruction [:rlc e] 4 1 (constantly "rlc e"))
   0x04 (->instruction [:rlc h] 4 1 (constantly "rlc h"))
   0x05 (->instruction [:rlc l] 4 1 (constantly "rlc l"))
   0x06 (->instruction [:rlc <hl>] 8 1 (constantly "rlc [hl]"))
   0x07 (->instruction [:rlc a] 4 1 (constantly "rlc a"))
   0x08 (->instruction [:rrc b] 4 1 (constantly "rrc b"))
   0x09 (->instruction [:rrc c] 4 1 (constantly "rrc c"))
   0x0A (->instruction [:rrc d] 4 1 (constantly "rrc d"))
   0x0B (->instruction [:rrc e] 4 1 (constantly "rrc e"))
   0x0C (->instruction [:rrc h] 4 1 (constantly "rrc h"))
   0x0D (->instruction [:rrc l] 4 1 (constantly "rrc l"))
   0x0E (->instruction [:rrc <hl>] 8 1 (constantly "rrc [hl]"))
   0x0F (->instruction [:rrc a] 4 1 (constantly "rrc a"))
   0x10 (->instruction [:rl b] 4 1 (constantly "rl b"))
   0x11 (->instruction [:rl c] 4 1 (constantly "rl c"))
   0x12 (->instruction [:rl d] 4 1 (constantly "rl d"))
   0x13 (->instruction [:rl e] 4 1 (constantly "rl e"))
   0x14 (->instruction [:rl h] 4 1 (constantly "rl h"))
   0x15 (->instruction [:rl l] 4 1 (constantly "rl l"))
   0x16 (->instruction [:rl <hl>] 8 1 (constantly "rl [hl]"))
   0x17 (->instruction [:rl a] 4 1 (constantly "rl a"))
   0x18 (->instruction [:rr b] 4 1 (constantly "rr b"))
   0x19 (->instruction [:rr c] 4 1 (constantly "rr c"))
   0x1A (->instruction [:rr d] 4 1 (constantly "rr d"))
   0x1B (->instruction [:rr e] 4 1 (constantly "rr e"))
   0x1C (->instruction [:rr h] 4 1 (constantly "rr h"))
   0x1D (->instruction [:rr l] 4 1 (constantly "rr l"))
   0x1E (->instruction [:rr <hl>] 8 1 (constantly "rr [hl]"))
   0x1F (->instruction [:rr a] 4 1 (constantly "rr a"))
   0x20 (->instruction [:sla b] 4 1 (constantly "sla b"))
   0x21 (->instruction [:sla c] 4 1 (constantly "sla c"))
   0x22 (->instruction [:sla d] 4 1 (constantly "sla d"))
   0x23 (->instruction [:sla e] 4 1 (constantly "sla e"))
   0x24 (->instruction [:sla h] 4 1 (constantly "sla h"))
   0x25 (->instruction [:sla l] 4 1 (constantly "sla l"))
   0x26 (->instruction [:sla <hl>] 8 1 (constantly "sla [hl]"))
   0x27 (->instruction [:sla a] 4 1 (constantly "sla a"))
   0x28 (->instruction [:sra b] 4 1 (constantly "sra b"))
   0x29 (->instruction [:sra c] 4 1 (constantly "sra c"))
   0x2A (->instruction [:sra d] 4 1 (constantly "sra d"))
   0x2B (->instruction [:sra e] 4 1 (constantly "sra e"))
   0x2C (->instruction [:sra h] 4 1 (constantly "sra h"))
   0x2D (->instruction [:sra l] 4 1 (constantly "sra l"))
   0x2E (->instruction [:sra <hl>] 8 1 (constantly "sra [hl]"))
   0x2F (->instruction [:sra a] 4 1 (constantly "sra a"))
   0x30 (->instruction [:swap b] 4 1 (constantly "swap b"))
   0x31 (->instruction [:swap c] 4 1 (constantly "swap c"))
   0x32 (->instruction [:swap d] 4 1 (constantly "swap d"))
   0x33 (->instruction [:swap e] 4 1 (constantly "swap e"))
   0x34 (->instruction [:swap h] 4 1 (constantly "swap h"))
   0x35 (->instruction [:swap l] 4 1 (constantly "swap l"))
   0x36 (->instruction [:swap <hl>] 8 1 (constantly "swap [hl]"))
   0x37 (->instruction [:swap a] 4 1 (constantly "swap a"))
   0x38 (->instruction [:srl b] 4 1 (constantly "srl b"))
   0x39 (->instruction [:srl c] 4 1 (constantly "srl c"))
   0x3A (->instruction [:srl d] 4 1 (constantly "srl d"))
   0x3B (->instruction [:srl e] 4 1 (constantly "srl e"))
   0x3C (->instruction [:srl h] 4 1 (constantly "srl h"))
   0x3D (->instruction [:srl l] 4 1 (constantly "srl l"))
   0x3E (->instruction [:srl <hl>] 8 1 (constantly "srl [hl]"))
   0x3F (->instruction [:srl a] 4 1 (constantly "srl a"))
   0x40 (->instruction [:bit 0 b] 4 1 (constantly "bit 0,b"))
   0x41 (->instruction [:bit 0 c] 4 1 (constantly "bit 0,c"))
   0x42 (->instruction [:bit 0 d] 4 1 (constantly "bit 0,d"))
   0x43 (->instruction [:bit 0 e] 4 1 (constantly "bit 0,e"))
   0x44 (->instruction [:bit 0 h] 4 1 (constantly "bit 0,h"))
   0x45 (->instruction [:bit 0 l] 4 1 (constantly "bit 0,l"))
   0x46 (->instruction [:bit 0 <hl>] 8 1 (constantly "bit 0,[hl]"))
   0x47 (->instruction [:bit 0 a] 4 1 (constantly "bit 0,a"))
   0x48 (->instruction [:bit 1 b] 4 1 (constantly "bit 1,b"))
   0x49 (->instruction [:bit 1 c] 4 1 (constantly "bit 1,c"))
   0x4A (->instruction [:bit 1 d] 4 1 (constantly "bit 1,d"))
   0x4B (->instruction [:bit 1 e] 4 1 (constantly "bit 1,e"))
   0x4C (->instruction [:bit 1 h] 4 1 (constantly "bit 1,h"))
   0x4D (->instruction [:bit 1 l] 4 1 (constantly "bit 1,l"))
   0x4E (->instruction [:bit 1 <hl>] 8 1 (constantly "bit 1,[hl]"))
   0x4F (->instruction [:bit 1 a] 4 1 (constantly "bit 1,a"))
   0x50 (->instruction [:bit 2 b] 4 1 (constantly "bit 2,b"))
   0x51 (->instruction [:bit 2 c] 4 1 (constantly "bit 2,c"))
   0x52 (->instruction [:bit 2 d] 4 1 (constantly "bit 2,d"))
   0x53 (->instruction [:bit 2 e] 4 1 (constantly "bit 2,e"))
   0x54 (->instruction [:bit 2 h] 4 1 (constantly "bit 2,h"))
   0x55 (->instruction [:bit 2 l] 4 1 (constantly "bit 2,l"))
   0x56 (->instruction [:bit 2 <hl>] 8 1 (constantly "bit 2,[hl]"))
   0x57 (->instruction [:bit 2 a] 4 1 (constantly "bit 2,a"))
   0x58 (->instruction [:bit 3 b] 4 1 (constantly "bit 3,b"))
   0x59 (->instruction [:bit 3 c] 4 1 (constantly "bit 3,c"))
   0x5A (->instruction [:bit 3 d] 4 1 (constantly "bit 3,d"))
   0x5B (->instruction [:bit 3 e] 4 1 (constantly "bit 3,e"))
   0x5C (->instruction [:bit 3 h] 4 1 (constantly "bit 3,h"))
   0x5D (->instruction [:bit 3 l] 4 1 (constantly "bit 3,l"))
   0x5E (->instruction [:bit 3 <hl>] 8 1 (constantly "bit 3,[hl]"))
   0x5F (->instruction [:bit 3 a] 4 1 (constantly "bit 3,a"))
   0x60 (->instruction [:bit 4 b] 4 1 (constantly "bit 4,b"))
   0x61 (->instruction [:bit 4 c] 4 1 (constantly "bit 4,c"))
   0x62 (->instruction [:bit 4 d] 4 1 (constantly "bit 4,d"))
   0x63 (->instruction [:bit 4 e] 4 1 (constantly "bit 4,e"))
   0x64 (->instruction [:bit 4 h] 4 1 (constantly "bit 4,h"))
   0x65 (->instruction [:bit 4 l] 4 1 (constantly "bit 4,l"))
   0x66 (->instruction [:bit 4 <hl>] 8 1 (constantly "bit 4,[hl]"))
   0x67 (->instruction [:bit 4 a] 4 1 (constantly "bit 4,a"))
   0x68 (->instruction [:bit 5 b] 4 1 (constantly "bit 5,b"))
   0x69 (->instruction [:bit 5 c] 4 1 (constantly "bit 5,c"))
   0x6A (->instruction [:bit 5 d] 4 1 (constantly "bit 5,d"))
   0x6B (->instruction [:bit 5 e] 4 1 (constantly "bit 5,e"))
   0x6C (->instruction [:bit 5 h] 4 1 (constantly "bit 5,h"))
   0x6D (->instruction [:bit 5 l] 4 1 (constantly "bit 5,l"))
   0x6E (->instruction [:bit 5 <hl>] 8 1 (constantly "bit 5,[hl]"))
   0x6F (->instruction [:bit 5 a] 4 1 (constantly "bit 5,a"))
   0x70 (->instruction [:bit 6 b] 4 1 (constantly "bit 6,b"))
   0x71 (->instruction [:bit 6 c] 4 1 (constantly "bit 6,c"))
   0x72 (->instruction [:bit 6 d] 4 1 (constantly "bit 6,d"))
   0x73 (->instruction [:bit 6 e] 4 1 (constantly "bit 6,e"))
   0x74 (->instruction [:bit 6 h] 4 1 (constantly "bit 6,h"))
   0x75 (->instruction [:bit 6 l] 4 1 (constantly "bit 6,l"))
   0x76 (->instruction [:bit 6 <hl>] 8 1 (constantly "bit 6,[hl]"))
   0x77 (->instruction [:bit 6 a] 4 1 (constantly "bit 6,a"))
   0x78 (->instruction [:bit 7 b] 4 1 (constantly "bit 7,b"))
   0x79 (->instruction [:bit 7 c] 4 1 (constantly "bit 7,c"))
   0x7A (->instruction [:bit 7 d] 4 1 (constantly "bit 7,d"))
   0x7B (->instruction [:bit 7 e] 4 1 (constantly "bit 7,e"))
   0x7C (->instruction [:bit 7 h] 4 1 (constantly "bit 7,h"))
   0x7D (->instruction [:bit 7 l] 4 1 (constantly "bit 7,l"))
   0x7E (->instruction [:bit 7 <hl>] 8 1 (constantly "bit 7,[hl]"))
   0x7F (->instruction [:bit 7 a] 4 1 (constantly "bit 7,a"))
   0x80 (->instruction [:res 0 b] 4 1 (constantly "res 0,b"))
   0x81 (->instruction [:res 0 c] 4 1 (constantly "res 0,c"))
   0x82 (->instruction [:res 0 d] 4 1 (constantly "res 0,d"))
   0x83 (->instruction [:res 0 e] 4 1 (constantly "res 0,e"))
   0x84 (->instruction [:res 0 h] 4 1 (constantly "res 0,h"))
   0x85 (->instruction [:res 0 l] 4 1 (constantly "res 0,l"))
   0x86 (->instruction [:res 0 <hl>] 8 1 (constantly "res 0,[hl]"))
   0x87 (->instruction [:res 0 a] 4 1 (constantly "res 0,a"))
   0x88 (->instruction [:res 1 b] 4 1 (constantly "res 1,b"))
   0x89 (->instruction [:res 1 c] 4 1 (constantly "res 1,c"))
   0x8A (->instruction [:res 1 d] 4 1 (constantly "res 1,d"))
   0x8B (->instruction [:res 1 e] 4 1 (constantly "res 1,e"))
   0x8C (->instruction [:res 1 h] 4 1 (constantly "res 1,h"))
   0x8D (->instruction [:res 1 l] 4 1 (constantly "res 1,l"))
   0x8E (->instruction [:res 1 <hl>] 8 1 (constantly "res 1,[hl]"))
   0x8F (->instruction [:res 1 a] 4 1 (constantly "res 1,a"))
   0x90 (->instruction [:res 2 b] 4 1 (constantly "res 2,b"))
   0x91 (->instruction [:res 2 c] 4 1 (constantly "res 2,c"))
   0x92 (->instruction [:res 2 d] 4 1 (constantly "res 2,d"))
   0x93 (->instruction [:res 2 e] 4 1 (constantly "res 2,e"))
   0x94 (->instruction [:res 2 h] 4 1 (constantly "res 2,h"))
   0x95 (->instruction [:res 2 l] 4 1 (constantly "res 2,l"))
   0x96 (->instruction [:res 2 <hl>] 8 1 (constantly "res 2,[hl]"))
   0x97 (->instruction [:res 2 a] 4 1 (constantly "res 2,a"))
   0x98 (->instruction [:res 3 b] 4 1 (constantly "res 3,b"))
   0x99 (->instruction [:res 3 c] 4 1 (constantly "res 3,c"))
   0x9A (->instruction [:res 3 d] 4 1 (constantly "res 3,d"))
   0x9B (->instruction [:res 3 e] 4 1 (constantly "res 3,e"))
   0x9C (->instruction [:res 3 h] 4 1 (constantly "res 3,h"))
   0x9D (->instruction [:res 3 l] 4 1 (constantly "res 3,l"))
   0x9E (->instruction [:res 3 <hl>] 8 1 (constantly "res 3,[hl]"))
   0x9F (->instruction [:res 3 a] 4 1 (constantly "res 3,a"))
   0xA0 (->instruction [:res 4 b] 4 1 (constantly "res 4,b"))
   0xA1 (->instruction [:res 4 c] 4 1 (constantly "res 4,c"))
   0xA2 (->instruction [:res 4 d] 4 1 (constantly "res 4,d"))
   0xA3 (->instruction [:res 4 e] 4 1 (constantly "res 4,e"))
   0xA4 (->instruction [:res 4 h] 4 1 (constantly "res 4,h"))
   0xA5 (->instruction [:res 4 l] 4 1 (constantly "res 4,l"))
   0xA6 (->instruction [:res 4 <hl>] 8 1 (constantly "res 4,[hl]"))
   0xA7 (->instruction [:res 4 a] 4 1 (constantly "res 4,a"))
   0xA8 (->instruction [:res 5 b] 4 1 (constantly "res 5,b"))
   0xA9 (->instruction [:res 5 c] 4 1 (constantly "res 5,c"))
   0xAA (->instruction [:res 5 d] 4 1 (constantly "res 5,d"))
   0xAB (->instruction [:res 5 e] 4 1 (constantly "res 5,e"))
   0xAC (->instruction [:res 5 h] 4 1 (constantly "res 5,h"))
   0xAD (->instruction [:res 5 l] 4 1 (constantly "res 5,l"))
   0xAE (->instruction [:res 5 <hl>] 8 1 (constantly "res 5,[hl]"))
   0xAF (->instruction [:res 5 a] 4 1 (constantly "res 5,a"))
   0xB0 (->instruction [:res 6 b] 4 1 (constantly "res 6,b"))
   0xB1 (->instruction [:res 6 c] 4 1 (constantly "res 6,c"))
   0xB2 (->instruction [:res 6 d] 4 1 (constantly "res 6,d"))
   0xB3 (->instruction [:res 6 e] 4 1 (constantly "res 6,e"))
   0xB4 (->instruction [:res 6 h] 4 1 (constantly "res 6,h"))
   0xB5 (->instruction [:res 6 l] 4 1 (constantly "res 6,l"))
   0xB6 (->instruction [:res 6 <hl>] 8 1 (constantly "res 6,[hl]"))
   0xB7 (->instruction [:res 6 a] 4 1 (constantly "res 6,a"))
   0xB8 (->instruction [:res 7 b] 4 1 (constantly "res 7,b"))
   0xB9 (->instruction [:res 7 c] 4 1 (constantly "res 7,c"))
   0xBA (->instruction [:res 7 d] 4 1 (constantly "res 7,d"))
   0xBB (->instruction [:res 7 e] 4 1 (constantly "res 7,e"))
   0xBC (->instruction [:res 7 h] 4 1 (constantly "res 7,h"))
   0xBD (->instruction [:res 7 l] 4 1 (constantly "res 7,l"))
   0xBE (->instruction [:res 7 <hl>] 8 1 (constantly "res 7,[hl]"))
   0xBF (->instruction [:res 7 a] 4 1 (constantly "res 7,a"))
   0xC0 (->instruction [:set 0 b] 4 1 (constantly "set 0,b"))
   0xC1 (->instruction [:set 0 c] 4 1 (constantly "set 0,c"))
   0xC2 (->instruction [:set 0 d] 4 1 (constantly "set 0,d"))
   0xC3 (->instruction [:set 0 e] 4 1 (constantly "set 0,e"))
   0xC4 (->instruction [:set 0 h] 4 1 (constantly "set 0,h"))
   0xC5 (->instruction [:set 0 l] 4 1 (constantly "set 0,l"))
   0xC6 (->instruction [:set 0 <hl>] 8 1 (constantly "set 0,[hl]"))
   0xC7 (->instruction [:set 0 a] 4 1 (constantly "set 0,a"))
   0xC8 (->instruction [:set 1 b] 4 1 (constantly "set 1,b"))
   0xC9 (->instruction [:set 1 c] 4 1 (constantly "set 1,c"))
   0xCA (->instruction [:set 1 d] 4 1 (constantly "set 1,d"))
   0xCB (->instruction [:set 1 e] 4 1 (constantly "set 1,e"))
   0xCC (->instruction [:set 1 h] 4 1 (constantly "set 1,h"))
   0xCD (->instruction [:set 1 l] 4 1 (constantly "set 1,l"))
   0xCE (->instruction [:set 1 <hl>] 8 1 (constantly "set 1,[hl]"))
   0xCF (->instruction [:set 1 a] 4 1 (constantly "set 1,a"))
   0xD0 (->instruction [:set 2 b] 4 1 (constantly "set 2,b"))
   0xD1 (->instruction [:set 2 c] 4 1 (constantly "set 2,c"))
   0xD2 (->instruction [:set 2 d] 4 1 (constantly "set 2,d"))
   0xD3 (->instruction [:set 2 e] 4 1 (constantly "set 2,e"))
   0xD4 (->instruction [:set 2 h] 4 1 (constantly "set 2,h"))
   0xD5 (->instruction [:set 2 l] 4 1 (constantly "set 2,l"))
   0xD6 (->instruction [:set 2 <hl>] 8 1 (constantly "set 2,[hl]"))
   0xD7 (->instruction [:set 2 a] 4 1 (constantly "set 2,a"))
   0xD8 (->instruction [:set 3 b] 4 1 (constantly "set 3,b"))
   0xD9 (->instruction [:set 3 c] 4 1 (constantly "set 3,c"))
   0xDA (->instruction [:set 3 d] 4 1 (constantly "set 3,d"))
   0xDB (->instruction [:set 3 e] 4 1 (constantly "set 3,e"))
   0xDC (->instruction [:set 3 h] 4 1 (constantly "set 3,h"))
   0xDD (->instruction [:set 3 l] 4 1 (constantly "set 3,l"))
   0xDE (->instruction [:set 3 <hl>] 8 1 (constantly "set 3,[hl]"))
   0xDF (->instruction [:set 3 a] 4 1 (constantly "set 3,a"))
   0xE0 (->instruction [:set 4 b] 4 1 (constantly "set 4,b"))
   0xE1 (->instruction [:set 4 c] 4 1 (constantly "set 4,c"))
   0xE2 (->instruction [:set 4 d] 4 1 (constantly "set 4,d"))
   0xE3 (->instruction [:set 4 e] 4 1 (constantly "set 4,e"))
   0xE4 (->instruction [:set 4 h] 4 1 (constantly "set 4,h"))
   0xE5 (->instruction [:set 4 l] 4 1 (constantly "set 4,l"))
   0xE6 (->instruction [:set 4 <hl>] 8 1 (constantly "set 4,[hl]"))
   0xE7 (->instruction [:set 4 a] 4 1 (constantly "set 4,a"))
   0xE8 (->instruction [:set 5 b] 4 1 (constantly "set 5,b"))
   0xE9 (->instruction [:set 5 c] 4 1 (constantly "set 5,c"))
   0xEA (->instruction [:set 5 d] 4 1 (constantly "set 5,d"))
   0xEB (->instruction [:set 5 e] 4 1 (constantly "set 5,e"))
   0xEC (->instruction [:set 5 h] 4 1 (constantly "set 5,h"))
   0xED (->instruction [:set 5 l] 4 1 (constantly "set 5,l"))
   0xEE (->instruction [:set 5 <hl>] 8 1 (constantly "set 5,[hl]"))
   0xEF (->instruction [:set 5 a] 4 1 (constantly "set 5,a"))
   0xF0 (->instruction [:set 6 b] 4 1 (constantly "set 6,b"))
   0xF1 (->instruction [:set 6 c] 4 1 (constantly "set 6,c"))
   0xF2 (->instruction [:set 6 d] 4 1 (constantly "set 6,d"))
   0xF3 (->instruction [:set 6 e] 4 1 (constantly "set 6,e"))
   0xF4 (->instruction [:set 6 h] 4 1 (constantly "set 6,h"))
   0xF5 (->instruction [:set 6 l] 4 1 (constantly "set 6,l"))
   0xF6 (->instruction [:set 6 <hl>] 8 1 (constantly "set 6,[hl]"))
   0xF7 (->instruction [:set 6 a] 4 1 (constantly "set 6,a"))
   0xF8 (->instruction [:set 7 b] 4 1 (constantly "set 7,b"))
   0xF9 (->instruction [:set 7 c] 4 1 (constantly "set 7,c"))
   0xFA (->instruction [:set 7 d] 4 1 (constantly "set 7,d"))
   0xFB (->instruction [:set 7 e] 4 1 (constantly "set 7,e"))
   0xFC (->instruction [:set 7 h] 4 1 (constantly "set 7,h"))
   0xFD (->instruction [:set 7 l] 4 1 (constantly "set 7,l"))
   0xFE (->instruction [:set 7 <hl>] 8 1 (constantly "set 7,[hl]"))
   0xFF (->instruction [:set 7 a] 4 1 (constantly "set 7,a"))})

(def decoder
  {0x00 (->instruction [:nop] 4 1 (constantly "nop"))
   0x01 (->instruction [:ld bc dword] 12 3 #(str "ld bc," (hex-dword %)))
   0x02 (->instruction [:ld <bc> a] 8 1 (constantly "ld [bc],a"))
   0x03 (->instruction [:inc16 bc] 8 1 (constantly "inc bc"))
   0x04 (->instruction [:inc b] 4 1 (constantly "inc b"))
   0x05 (->instruction [:dec b] 4 1 (constantly "dec b"))
   0x06 (->instruction [:ld b word] 8 2 #(str "ld b," (hex-word %)))
   0x07 (->instruction [:rlca] 4 1 (constantly "rlca"))
   0x08 (->instruction [:ld address sp] 20 3 #(str "ld [" (hex-dword %) "],sp"))
   0x09 (->instruction [:add-hl bc] 8 1 (constantly "add hl,bc"))
   0x0A (->instruction [:ld a <bc>] 8 1 (constantly "ld a,[bc]"))
   0x0C (->instruction [:inc c] 4 1 (constantly "inc c"))
   0x0D (->instruction [:dec c] 4 1 (constantly "dec c"))
   0x0E (->instruction [:ld c word] 8 2 #(str "ld c," (hex-word %)))
   0x0F (->instruction [:rrca] 4 1 (constantly "rrca"))
   0x10 (->instruction [:stop 0] 4 1 (constantly "stop"))
   0x11 (->instruction [:ld de dword] 12 3 #(str "ld de," (hex-dword %)))
   0x12 (->instruction [:ld <de> a] 8 1 (constantly "ld [de],8"))
   0x13 (->instruction [:inc16 de] 8 1 (constantly "inc de"))
   0x14 (->instruction [:inc d] 4 1 (constantly "inc d"))
   0x15 (->instruction [:dec d] 4 1 (constantly "dec d"))
   0x16 (->instruction [:ld d word] 8 2 #(str "ld d," (hex-word %)))
   0x17 (->instruction [:rla] 4 1 #(constantly "rla"))
   0x18 (->instruction [:jr always word] 12 2 #(str "jr " (hex-word %)))
   0x19 (->instruction [:add-hl de] 8 1 (constantly "add hl,de"))
   0x1A (->instruction [:ld a <de>] 8 1 (constantly "ld a,[de]"))
   0x1C (->instruction [:inc e] 4 1 (constantly "inc e"))
   0x1D (->instruction [:dec e] 4 1 (constantly "dec e"))
   0x1E (->instruction [:ld e word] 8 2 #(str "ld e," (hex-word %)))
   0x1F (->instruction [:rra] 4 1 (constantly "rra"))
   0x20 (->instruction [:jr nz? word] [12 8] 2 #(str "jr nz " (hex-word %)))
   0x21 (->instruction [:ld hl dword] 12 3 #(str "ld hl," (hex-dword %)))
   0x22 (->instruction [:ldi <hl> a] 8 1 (constantly "ldi [hl],a"))
   0x23 (->instruction [:inc16 hl] 8 1 (constantly "inc hl"))
   0x24 (->instruction [:inc h] 4 1 (constantly "inc h"))
   0x25 (->instruction [:dec h] 4 1 (constantly "dec h"))
   0x26 (->instruction [:ld h word] 8 2 #(str "ld h," (hex-word %)))
   0x28 (->instruction [:jr z? word] [12 8] 2 #(str "jr z " (hex-word %)))
   0x29 (->instruction [:add-hl hl] 8 1 (constantly "add hl,hl"))
   0x2A (->instruction [:ldi a <hl>] 8 1 (constantly "ldi a,[hl]"))
   0x2C (->instruction [:inc l] 4 1 (constantly "inc l"))
   0x2D (->instruction [:dec l] 4 1 (constantly "dec l"))
   0x2E (->instruction [:ld l word] 8 2 #(str "ld l," (hex-word %)))
   0x30 (->instruction [:jr nc? word] [12 8] 2 #(str "jr nc " (hex-word %)))
   0x31 (->instruction [:ld sp dword] 12 3 #(str "ld sp," (hex-dword %)))
   0x32 (->instruction [:ldd <hl> a] 8 1 (constantly "ldd [hl],a"))
   0x33 (->instruction [:inc16 sp] 8 1 (constantly "inc sp"))
   0x34 (->instruction [:inc <hl>] 12 1 (constantly "inc [hl]"))
   0x35 (->instruction [:dec <hl>] 4 1 (constantly "dec [hl]"))
   0x38 (->instruction [:jr c? word] [12 8] 2 #(str "jr c " (hex-word %)))
   0x39 (->instruction [:add-hl bc] 8 1 (constantly "add hl,sp"))
   0x3A (->instruction [:ldd a <hl>] 8 1 (constantly "ldd a,[hl]"))
   0x3C (->instruction [:inc a] 4 1 (constantly "inc a"))
   0x3D (->instruction [:dec a] 4 1 (constantly "dec a"))
   0x3E (->instruction [:ld a word] 8 2 #(str "ld a," (hex-word %)))
   0x40 (->instruction [:ld b b] 4 1 (constantly "ld b,b"))
   0x41 (->instruction [:ld b c] 4 1 (constantly "ld b,c"))
   0x42 (->instruction [:ld b d] 4 1 (constantly "ld b,d"))
   0x43 (->instruction [:ld b e] 4 1 (constantly "ld b,e"))
   0x44 (->instruction [:ld b h] 4 1 (constantly "ld b,h"))
   0x45 (->instruction [:ld b l] 4 1 (constantly "ld b,l"))
   0x46 (->instruction [:ld b <hl>] 4 1 (constantly "ld b,[hl]"))
   0x47 (->instruction [:ld b a] 4 1 (constantly "ld b,a"))
   0x48 (->instruction [:ld c b] 4 1 (constantly "ld c,b"))
   0x49 (->instruction [:ld c c] 4 1 (constantly "ld c,c"))
   0x4A (->instruction [:ld c d] 4 1 (constantly "ld c,d"))
   0x4B (->instruction [:ld c e] 4 1 (constantly "ld c,e"))
   0x4C (->instruction [:ld c h] 4 1 (constantly "ld c,h"))
   0x4D (->instruction [:ld c l] 4 1 (constantly "ld c,l"))
   0x4E (->instruction [:ld c <hl>] 4 1 (constantly "ld c,[hl]"))
   0x4F (->instruction [:ld c a] 4 1 (constantly "ld c,a"))
   0x50 (->instruction [:ld d b] 4 1 (constantly "ld d,b"))
   0x51 (->instruction [:ld d c] 4 1 (constantly "ld d,c"))
   0x52 (->instruction [:ld d d] 4 1 (constantly "ld d,d"))
   0x53 (->instruction [:ld d e] 4 1 (constantly "ld d,e"))
   0x54 (->instruction [:ld d h] 4 1 (constantly "ld d,h"))
   0x55 (->instruction [:ld d l] 4 1 (constantly "ld d,l"))
   0x56 (->instruction [:ld d <hl>] 4 1 (constantly "ld d,[hl]"))
   0x57 (->instruction [:ld d a] 4 1 (constantly "ld d,a"))
   0x58 (->instruction [:ld e b] 4 1 (constantly "ld e,b"))
   0x59 (->instruction [:ld e c] 4 1 (constantly "ld e,c"))
   0x5A (->instruction [:ld e d] 4 1 (constantly "ld e,d"))
   0x5B (->instruction [:ld e e] 4 1 (constantly "ld e,e"))
   0x5C (->instruction [:ld e h] 4 1 (constantly "ld e,h"))
   0x5D (->instruction [:ld e l] 4 1 (constantly "ld e,l"))
   0x5E (->instruction [:ld e <hl>] 4 1 (constantly "ld e,[hl]"))
   0x5F (->instruction [:ld e a] 4 1 (constantly "ld e,a"))
   0x60 (->instruction [:ld h b] 4 1 (constantly "ld h,b"))
   0x61 (->instruction [:ld h c] 4 1 (constantly "ld h,c"))
   0x62 (->instruction [:ld h d] 4 1 (constantly "ld h,d"))
   0x63 (->instruction [:ld h e] 4 1 (constantly "ld h,e"))
   0x64 (->instruction [:ld h h] 4 1 (constantly "ld h,h"))
   0x65 (->instruction [:ld h l] 4 1 (constantly "ld h,l"))
   0x66 (->instruction [:ld h <hl>] 4 1 (constantly "ld h,[hl]"))
   0x67 (->instruction [:ld h a] 4 1 (constantly "ld h,a"))
   0x68 (->instruction [:ld l b] 4 1 (constantly "ld l,b"))
   0x69 (->instruction [:ld l c] 4 1 (constantly "ld l,c"))
   0x6A (->instruction [:ld l d] 4 1 (constantly "ld l,d"))
   0x6B (->instruction [:ld l e] 4 1 (constantly "ld l,e"))
   0x6C (->instruction [:ld l h] 4 1 (constantly "ld l,h"))
   0x6D (->instruction [:ld l l] 4 1 (constantly "ld l,l"))
   0x6E (->instruction [:ld l <hl>] 4 1 (constantly "ld l,[hl]"))
   0x6F (->instruction [:ld l a] 4 1 (constantly "ld l,a"))
   0x70 (->instruction [:ld <hl> b] 4 1 (constantly "ld [hl],b"))
   0x71 (->instruction [:ld <hl> c] 4 1 (constantly "ld [hl],c"))
   0x72 (->instruction [:ld <hl> d] 4 1 (constantly "ld [hl],d"))
   0x73 (->instruction [:ld <hl> e] 4 1 (constantly "ld [hl],e"))
   0x74 (->instruction [:ld <hl> h] 4 1 (constantly "ld [hl],h"))
   0x75 (->instruction [:ld <hl> l] 4 1 (constantly "ld [hl],l"))
   0x77 (->instruction [:ld <hl> a] 4 1 (constantly "ld [hl],a"))
   0x78 (->instruction [:ld a b] 4 1 (constantly "ld a,b"))
   0x79 (->instruction [:ld a c] 4 1 (constantly "ld a,c"))
   0x7A (->instruction [:ld a d] 4 1 (constantly "ld a,d"))
   0x7B (->instruction [:ld a e] 4 1 (constantly "ld a,e"))
   0x7C (->instruction [:ld a h] 4 1 (constantly "ld a,h"))
   0x7D (->instruction [:ld a l] 4 1 (constantly "ld a,l"))
   0x7E (->instruction [:ld a <hl>] 8 1 (constantly "ld a,[hl]"))
   0x7F (->instruction [:ld a a] 4 1 (constantly "ld a,a"))
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
   0xCF (->instruction [:rst 0x08] 16 1 (constantly "rst 08"))
   0xC8 (->instruction [:ret z?] [20 8] 1 (constantly "ret z"))
   0xC9 (->instruction [:ret always] 16 1 (constantly "ret"))
   0xCB (->instruction [:extra word] 4 1 #(:to-string (extra-decoder (word %)))) ;; Size is 1 because extra instructions have size 1 too
   0xCC (->instruction [:call z? address] 24 3 #(str "call z " (hex-dword %)))
   0xCD (->instruction [:call always address] 24 3 #(str "call " (hex-dword %)))
   0xCE (->instruction [:adc word] 8 2 #(str "adc " (hex-word %)))
   0xD0 (->instruction [:ret nc?] [20 8] 1 (constantly "ret nc"))
   0xD1 (->instruction [:pop de] 12 1 (constantly "pop de"))
   0xD4 (->instruction [:call nc? address] 24 3 #(str "call nc " (hex-dword %)))
   0xD5 (->instruction [:push de] 16 1 (constantly "push de"))
   0xD6 (->instruction [:sub word] 8 2 #(str "sub " (hex-word %)))
   0xD7 (->instruction [:rst 0x10] 16 1 (constantly "rst 10"))
   0xDC (->instruction [:call c? address] 24 3 #(str "call " (hex-dword %)))
   0xDE (->instruction [:sbc word] 8 2 #(str "sbc " (hex-word %)))
   0xDF (->instruction [:rst 0x18] 16 1 (constantly "rst 18"))
   0xE0 (->instruction [:ld <FF00+n> a] 12 2 #(str "ldh [FF00+" (hex-word %) "],a"))
   0xE1 (->instruction [:pop hl] 12 1 (constantly "pop hl"))
   0xEA (->instruction [:ld <address> a] 16 3 #(str "ld [" (hex-dword %) "],a"))
   0xE5 (->instruction [:push hl] 16 1 (constantly "push hl"))
   0xE6 (->instruction [:and word] 4 2 #(str "and " (hex-word %)))
   0xE7 (->instruction [:rst 0x20] 16 1 (constantly "rst 20"))
   0xE8 (->instruction [:add-sp word] 16 2 #(str "add sp," (hex-word %)))
   0xEE (->instruction [:xor word] 8 2 #(str "xor " (hex-word %)))
   0xEF (->instruction [:rst 0x28] 16 1 (constantly "rst 28"))
   0xF0 (->instruction [:ld a <FF00+n>] 12 2 #(str "ldh a,[FF00+" (hex-word %) "]"))
   0xF1 (->instruction [:pop af] 12 1 (constantly "pop af"))
   0xF3 (->instruction [:di] 4 1 (constantly "di"))
   0xF5 (->instruction [:push af] 16 1 (constantly "push af"))
   0xF7 (->instruction [:rst 0x30] 16 1 (constantly "rst 30"))
   0xFA (->instruction [:ld a <address>] 12 3 #(str "ldh a,[" (hex-dword %) "]"))
   0xFB (->instruction [:ei] 4 1 (constantly "ei"))
   0xFE (->instruction [:cp word] 8 2 #(str "cp " (hex-word %)))
   0xFF (->instruction [:rst 0x38] 16 1 (constantly "rst 38"))})

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