(ns repicene.decoder
  (:require [repicene.schema :as s :refer [dword? word?]]))

(def hex8 (partial format "0x%02X"))
(def hex16 (partial format "0x%04X"))
(defn cat8
  "concatenate two words to make a double"
  [x y]
  {:pre [(<= 0 x 255) (<= 0 y 255)]}
  (bit-or (bit-shift-left x 8) y))

(defn in? [[from to _] address]
  (<= from address to))

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
       (bit-or dword))))

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
  (fn [cpu]
    (zero? (bit-and pos (f cpu)))))

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
   0x06 (->instruction [:ld b word] 8 2 #(str "ld b," (hex-word %)))
   0x0E (->instruction [:ld c word] 8 2 #(str "ld c," (hex-word %)))
   0x11 (->instruction [:ld de dword] 12 3 #(str "ld de," (hex-dword %)))
   0x16 (->instruction [:ld d word] 8 2 #(str "ld d," (hex-word %)))
   0x1E (->instruction [:ld e word] 8 2 #(str "ld e," (hex-word %)))
   0x21 (->instruction [:ld hl dword] 12 3 #(str "ld hl," (hex-dword %)))
   0x26 (->instruction [:ld h word] 8 2 #(str "ld h," (hex-word %)))
   0x2E (->instruction [:ld l word] 8 2 #(str "ld l," (hex-word %)))
   0x31 (->instruction [:ld sp dword] 12 3 #(str "ld sp," (hex-dword %)))
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
   0x7E (->instruction [:ld a <hl>] 4 1 (constantly "ld a,<hl>"))
   0x7F (->instruction [:ld a a] 4 1 (constantly "ld a,a"))
   0xC0 (->instruction [:ret-cond :nz address] [20 8] 3 #(str "ret nz " (hex-dword %)))
   0xC2 (->instruction [:jp nz? address] [16 12] 3 #(str "jp nz " (hex-dword %)))
   0xC3 (->instruction [:jp always address] 8 3 #(str "jp " (hex-dword %)))
   0xC4 (->instruction [:call nz? address] 24 3 #(str "call nz" (hex-dword %)))
   0xCC (->instruction [:call z? address] 24 3 #(str "call z" (hex-dword %)))
   0xCD (->instruction [:call always address] 24 3 #(str "call " (hex-dword %)))
   0xD4 (->instruction [:call nc? address] 24 3 #(str "call nc " (hex-dword %)))
   0xDC (->instruction [:call c? address] 24 3 #(str "call " (hex-dword %)))
   0xE0 (->instruction [:ld <FF00+n> a] 12 2 #(str "ldh [FF00+" (hex-word %) "],a"))
   0xEA (->instruction [:ld <address> a] 16 3 #(str "ld [" (hex-dword %) "],a"))
   0xF3 (->instruction [:di] 4 1 (constantly "di"))
   0xFB (->instruction [:ei] 4 1 (constantly "ei"))
   0xFE (->instruction [:ld a <FF00+n>] 12 2 #(str "ldh a,[FF00+" (hex-word %) "]"))})
