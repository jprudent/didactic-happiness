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

(def %16inc
  "Increment parameter and make it a valid address (mod 0xFFFF)"
  (partial %16+ 1))

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
   (cat8 (word-at memory (%16+ 1 address)) (word-at memory address))))            ;; dword are stored little endian

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
     {:pre [(s/valid? cpu) (boolean? set?)]
      :post [(s/valid? %)]}
     (if (= (bit-test (f cpu) pos) set?)
       cpu
       (f cpu (%8 bit-flip (f cpu) pos))))))

(def z? (def-flag 2r10000000))
(def n? (def-flag 2r01000000))
(def h? (def-flag 2r00100000))
(def c? (def-flag 2r00010000))
(def nz? (complement z?))
(def nc? (complement c?))

(defn set-word-at [{:keys [::s/memory] :as cpu} address val]
  {:pre [(dword? address) (word? val)]}
  (println "word-at " (hex16 address))
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

(defn <hl>
  ([{:keys [::s/memory] :as cpu}]
   (word-at memory (hl cpu)))
  ([cpu val]
   (set-word-at cpu (hl cpu) val)))

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

(def decoder
  {0x00 (->instruction [:nop] 4 1 (constantly "nop"))
   0x01 (->instruction [:ld bc dword] 12 3 #(str "ld bc," (hex-dword %)))
   0x03 (->instruction [:inc bc] 8 1 (constantly "inc bc"))
   0x06 (->instruction [:ld b word] 8 2 #(str "ld b," (hex-word %)))
   0x0E (->instruction [:ld c word] 8 2 #(str "ld c," (hex-word %)))
   0x10 (->instruction [:stop 0] 4 1 (constantly "stop"))
   0x11 (->instruction [:ld de dword] 12 3 #(str "ld de," (hex-dword %)))
   0x13 (->instruction [:inc de] 8 1 (constantly "inc de"))
   0x16 (->instruction [:ld d word] 8 2 #(str "ld d," (hex-word %)))
   0x18 (->instruction [:jr always word] 12 2 #(str "jr " (hex-word %)))
   0x1E (->instruction [:ld e word] 8 2 #(str "ld e," (hex-word %)))
   0x20 (->instruction [:jr nz? word] [12 8] 2 #(str "jr nz " (hex-word %)))
   0x21 (->instruction [:ld hl dword] 12 3 #(str "ld hl," (hex-dword %)))
   0x23 (->instruction [:inc hl] 8 1 (constantly "inc hl"))
   0x26 (->instruction [:ld h word] 8 2 #(str "ld h," (hex-word %)))
   0x28 (->instruction [:jr z? word] [12 8] 2 #(str "jr z " (hex-word %)))
   0x2A (->instruction [:ldi a <hl>] 8 1 (constantly "ldi a,[hl]"))
   0x2E (->instruction [:ld l word] 8 2 #(str "ld l," (hex-word %)))
   0x30 (->instruction [:jr nc? word] [12 8] 2 #(str "jr nc " (hex-word %)))
   0x31 (->instruction [:ld sp dword] 12 3 #(str "ld sp," (hex-dword %)))
   0x33 (->instruction [:inc sp] 8 1 (constantly "inc sp"))
   0x38 (->instruction [:jr c? word] [12 8] 2 #(str "jr c " (hex-word %)))
   0x3E (->instruction [:ld a word] 8 2 #(str "ld a," (hex-word %)))
   0x40 (->instruction [:ld b b] 4 1 (constantly "ld b,b"))
   0x41 (->instruction [:ld b c] 4 1 (constantly "ld b,c"))
   0x42 (->instruction [:ld b d] 4 1 (constantly "ld b,d"))
   0x43 (->instruction [:ld b e] 4 1 (constantly "ld b,e"))
   0x44 (->instruction [:ld b h] 4 1 (constantly "ld b,h"))
   0x45 (->instruction [:ld b l] 4 1 (constantly "ld b,l"))
   0x46 (->instruction [:ld b <hl>] 4 1 (constantly "ld b,<hl>"))
   0x47 (->instruction [:ld b a] 4 1 (constantly "ld b,a"))
   0x48 (->instruction [:ld c b] 4 1 (constantly "ld c,b"))
   0x49 (->instruction [:ld c c] 4 1 (constantly "ld c,c"))
   0x4A (->instruction [:ld c d] 4 1 (constantly "ld c,d"))
   0x4B (->instruction [:ld c e] 4 1 (constantly "ld c,e"))
   0x4C (->instruction [:ld c h] 4 1 (constantly "ld c,h"))
   0x4D (->instruction [:ld c l] 4 1 (constantly "ld c,l"))
   0x4E (->instruction [:ld c <hl>] 4 1 (constantly "ld c,<hl>"))
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
   0x5E (->instruction [:ld e <hl>] 4 1 (constantly "ld e,<hl>"))
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
   0x6E (->instruction [:ld l <hl>] 4 1 (constantly "ld l,<hl>"))
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
   0x7E (->instruction [:ld a <hl>] 8 1 (constantly "ld a,<hl>"))
   0x7F (->instruction [:ld a a] 4 1 (constantly "ld a,a"))
   0x90 (->instruction [:sub b] 4 1 (constantly "sub b"))
   0x91 (->instruction [:sub c] 4 1 (constantly "sub c"))
   0x92 (->instruction [:sub d] 4 1 (constantly "sub d"))
   0x93 (->instruction [:sub e] 4 1 (constantly "sub e"))
   0x94 (->instruction [:sub h] 4 1 (constantly "sub h"))
   0x95 (->instruction [:sub l] 4 1 (constantly "sub l"))
   0x96 (->instruction [:sub <hl>] 8 1 (constantly "sub [hl]"))
   0x97 (->instruction [:sub a] 4 1 (constantly "sub a"))
   0xB0 (->instruction [:or b] 4 1 (constantly "or b"))
   0xB1 (->instruction [:or c] 4 1 (constantly "or c"))
   0xB2 (->instruction [:or d] 4 1 (constantly "or d"))
   0xB3 (->instruction [:or e] 4 1 (constantly "or e"))
   0xB4 (->instruction [:or h] 4 1 (constantly "or h"))
   0xB5 (->instruction [:or l] 4 1 (constantly "or l"))
   0xB6 (->instruction [:or <hl>] 4 1 (constantly "or (hl)"))
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
   0xC7 (->instruction [:rst 0x00] 16 1 (constantly "rst 00"))
   0xCF (->instruction [:rst 0x08] 16 1 (constantly "rst 08"))
   0xC9 (->instruction [:ret always] 16 1 (constantly "ret"))
   0xCC (->instruction [:call z? address] 24 3 #(str "call z " (hex-dword %)))
   0xCD (->instruction [:call always address] 24 3 #(str "call " (hex-dword %)))
   0xD0 (->instruction [:ret nc?] [20 8] 1 (constantly "ret nc"))
   0xD1 (->instruction [:pop de] 12 1 (constantly "pop de"))
   0xD4 (->instruction [:call nc? address] 24 3 #(str "call nc " (hex-dword %)))
   0xD5 (->instruction [:push de] 16 1 (constantly "push de"))
   0xD6 (->instruction [:sub word] 8 2 #(str "sub " (hex-word %)))
   0xD7 (->instruction [:rst 0x10] 16 1 (constantly "rst 10"))
   0xDC (->instruction [:call c? address] 24 3 #(str "call " (hex-dword %)))
   0xDF (->instruction [:rst 0x18] 16 1 (constantly "rst 18"))
   0xE0 (->instruction [:ld <FF00+n> a] 12 2 #(str "ldh [FF00+" (hex-word %) "],a"))
   0xE1 (->instruction [:pop hl] 12 1 (constantly "pop hl"))
   0xEA (->instruction [:ld <address> a] 16 3 #(str "ld [" (hex-dword %) "],a"))
   0xE5 (->instruction [:push hl] 16 1 (constantly "push hl"))
   0xE7 (->instruction [:rst 0x20] 16 1 (constantly "rst 20"))
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
