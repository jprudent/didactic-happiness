(ns repicene.decoder)

(defn dword? [x] (<= 0 x 0xFFFF))
(defn word? [x] (<= 0 x 0xFF))

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
   {:pre [(<= 0 address 0xFFFF)]}
   (when-let [[from _ backend] (lookup-backend memory address)]
     (let [backend-relative-address (- address from)]
       (get backend backend-relative-address)))))

(defn high-word
  "1 arg version : returns the high word composing the unsigned dword
  2 args version : set the high word of dword to val"
  ([dword]
   {:pre [(<= 0 dword 0xFFFF)]}
   (bit-shift-right dword 8))
  ([dword val]
   {:pre [(dword? dword) (word? val)]}
   (-> (bit-shift-left val 8)
       (bit-or dword))))

(defn low-word
  "1 arg version : returns the low word composing the unsigned dword
  2 args version : set the low word of dword to val"
  ([dword]
   {:pre [(<= 0 dword 0xFFFF)]}
   (bit-and dword 0xFF))
  ([dword val]
   {:pre [(dword? dword) (word? val)]}
   (bit-or (bit-and dword 0xFF00) val)))

(defn def-dword-register [register]
  (fn
    ([cpu]
     (get-in cpu [:registers register]))
    ([cpu modifier]
     {:pre [(not (nil? cpu)) (not (nil? (:memory cpu))) (or (fn? modifier) (dword? modifier))]}
     (if (fn? modifier)
       (update-in cpu [:registers register] modifier)
       (assoc-in cpu [:registers register] modifier)))))

(defn def-word-register [high-or-low dword-register]
  (fn
    ([cpu] (high-or-low (dword-register cpu)))
    ([cpu val] (dword-register cpu (high-or-low (dword-register cpu) val)))))

(def pc (def-dword-register :PC))
(def sp (def-dword-register :SP))
(def af (def-dword-register :AF))
(def bc (def-dword-register :BC))
(def de (def-dword-register :DE))
(def hl (def-dword-register :HL))

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

(def z (def-flag 2r10000000))
(def n (def-flag 2r01000000))
(def h (def-flag 2r00100000))
(def c (def-flag 2r00010000))
(def nz (complement z))
(def nc (complement c))

(defn set-word-at [{:keys [memory] :as cpu} address val]
  {:pre [(dword? address) (word? val)]}
  (println "word-at " (hex16 address))
  (let [[index [from & _]] (lookup-backend-index memory address)
        backend-relative-address (- address from)]
    (update-in cpu [:memory index 2] assoc backend-relative-address val)))

(defn set-dword-at [{:keys [memory] :as cpu} address val]
  {:pre [(dword? address) (dword? val)]}
  (-> (set-word-at cpu address (low-word val))
      (set-word-at (inc address) (high-word val))))

(defn dword
  ([{:keys [memory] :as cpu}]
   {:pre [(not (nil? cpu)) (not (nil? memory))]}
   (cat8 (word-at memory (+ 2 (pc cpu)))
         (word-at memory (+ 1 (pc cpu)))))
  ([cpu val]
   (set-word-at cpu (dword cpu) val)))

(defn word
  [{:keys [memory] :as cpu}]
  (word-at memory (inc (pc cpu))))

(defn <FF00+n>
  ([{:keys [memory] :as cpu}]
   (word-at memory (+ 0xFF00 (word cpu))))
  ([cpu val]
   (set-word-at cpu (+ 0xFF00 (word cpu)) val)))


(defn <hl>
  ([{:keys [memory] :as cpu}]
   (word-at memory (hl cpu)))
  ([cpu val]
   (set-word-at cpu (hl cpu) val)))

;; synonyms to make the code more friendly
(def <address> dword)
(def address dword)


(def always (constantly true))

(def hex-dword (comp hex16 dword))
(def hex-word (comp hex8 word))

(defn fetch [{:keys [memory] :as cpu}]
  (println "fetched" (hex8 (word-at memory (pc cpu))))
  (word-at memory (pc cpu)))

(def decoder
  {0x00 [:nop 4 1 (constantly "nop")]
   0x01 [:ld bc dword 12 3 #(str "ld bc," (hex-dword %))]
   0x06 [:ld b word 8 2 #(str "ld b," (hex-word %))]
   0x0E [:ld c word 8 2 #(str "ld c," (hex-word %))]
   0x11 [:ld de dword 12 3 #(str "ld de," (hex-dword %))]
   0x16 [:ld d word 8 2 #(str "ld d," (hex-word %))]
   0x1E [:ld e word 8 2 #(str "ld e," (hex-word %))]
   0x21 [:ld hl dword 12 3 #(str "ld hl," (hex-dword %))]
   0x26 [:ld h word 8 2 #(str "ld h," (hex-word %))]
   0x2E [:ld l word 8 2 #(str "ld l," (hex-word %))]
   0x31 [:ld sp dword 12 3 #(str "ld sp," (hex-dword %))]
   0x3E [:ld a word 8 2 #(str "ld a," (hex-word %))]
   0x40 [:ld b b 4 1 (constantly "ld b,b")]
   0x41 [:ld b c 4 1 (constantly "ld b,c")]
   0x42 [:ld b d 4 1 (constantly "ld b,d")]
   0x43 [:ld b e 4 1 (constantly "ld b,e")]
   0x44 [:ld b h 4 1 (constantly "ld b,h")]
   0x45 [:ld b l 4 1 (constantly "ld b,l")]
   0x46 [:ld b <hl> 4 1 (constantly "ld b,<hl>")]
   0x47 [:ld b a 4 1 (constantly "ld b,a")]
   0x48 [:ld c b 4 1 (constantly "ld c,b")]
   0x49 [:ld c c 4 1 (constantly "ld c,c")]
   0x4A [:ld c d 4 1 (constantly "ld c,d")]
   0x4B [:ld c e 4 1 (constantly "ld c,e")]
   0x4C [:ld c h 4 1 (constantly "ld c,h")]
   0x4D [:ld c l 4 1 (constantly "ld c,l")]
   0x4E [:ld c <hl> 4 1 (constantly "ld c,<hl>")]
   0x4F [:ld c a 4 1 (constantly "ld c,a")]
   0x50 [:ld d b 4 1 (constantly "ld d,b")]
   0x51 [:ld d c 4 1 (constantly "ld d,c")]
   0x52 [:ld d d 4 1 (constantly "ld d,d")]
   0x53 [:ld d e 4 1 (constantly "ld d,e")]
   0x54 [:ld d h 4 1 (constantly "ld d,h")]
   0x55 [:ld d l 4 1 (constantly "ld d,l")]
   0x56 [:ld d <hl> 4 1 (constantly "ld d,[hl]")]
   0x57 [:ld d a 4 1 (constantly "ld d,a")]
   0x58 [:ld e b 4 1 (constantly "ld e,b")]
   0x59 [:ld e c 4 1 (constantly "ld e,c")]
   0x5A [:ld e d 4 1 (constantly "ld e,d")]
   0x5B [:ld e e 4 1 (constantly "ld e,e")]
   0x5C [:ld e h 4 1 (constantly "ld e,h")]
   0x5D [:ld e l 4 1 (constantly "ld e,l")]
   0x5E [:ld e <hl> 4 1 (constantly "ld e,<hl>")]
   0x5F [:ld e a 4 1 (constantly "ld e,a")]
   0x60 [:ld h b 4 1 (constantly "ld h,b")]
   0x61 [:ld h c 4 1 (constantly "ld h,c")]
   0x62 [:ld h d 4 1 (constantly "ld h,d")]
   0x63 [:ld h e 4 1 (constantly "ld h,e")]
   0x64 [:ld h h 4 1 (constantly "ld h,h")]
   0x65 [:ld h l 4 1 (constantly "ld h,l")]
   0x66 [:ld h <hl> 4 1 (constantly "ld h,[hl]")]
   0x67 [:ld h a 4 1 (constantly "ld h,a")]
   0x68 [:ld l b 4 1 (constantly "ld l,b")]
   0x69 [:ld l c 4 1 (constantly "ld l,c")]
   0x6A [:ld l d 4 1 (constantly "ld l,d")]
   0x6B [:ld l e 4 1 (constantly "ld l,e")]
   0x6C [:ld l h 4 1 (constantly "ld l,h")]
   0x6D [:ld l l 4 1 (constantly "ld l,l")]
   0x6E [:ld l <hl> 4 1 (constantly "ld l,<hl>")]
   0x6F [:ld l a 4 1 (constantly "ld l,a")]
   0x70 [:ld <hl> b 4 1 (constantly "ld [hl],b")]
   0x71 [:ld <hl> c 4 1 (constantly "ld [hl],c")]
   0x72 [:ld <hl> d 4 1 (constantly "ld [hl],d")]
   0x73 [:ld <hl> e 4 1 (constantly "ld [hl],e")]
   0x74 [:ld <hl> h 4 1 (constantly "ld [hl],h")]
   0x75 [:ld <hl> l 4 1 (constantly "ld [hl],l")]
   0x77 [:ld <hl> a 4 1 (constantly "ld [hl],a")]
   0x78 [:ld a b 4 1 (constantly "ld a,b")]
   0x79 [:ld a c 4 1 (constantly "ld a,c")]
   0x7A [:ld a d 4 1 (constantly "ld a,d")]
   0x7B [:ld a e 4 1 (constantly "ld a,e")]
   0x7C [:ld a h 4 1 (constantly "ld a,h")]
   0x7D [:ld a l 4 1 (constantly "ld a,l")]
   0x7E [:ld a <hl> 4 1 (constantly "ld a,<hl>")]
   0x7F [:ld a a 4 1 (constantly "ld a,a")]
   0xC0 [:ret-cond :nz address [20 8] 3 #(str "ret nz " (hex-dword %))]
   0xC2 [:jp nz address [16 12] 3 #(str "jp nz " (hex-dword %))]
   0xC3 [:jp always address 8 3 #(str "jp " (hex-dword %))]
   0xC4 [:call nz address 24 3 #(str "call nz" (hex-dword %))]
   0xCC [:call z address 24 3 #(str "call z" (hex-dword %))]
   0xCD [:call always address 24 3 #(str "call " (hex-dword %))]
   0xD4 [:call nc address 24 3 #(str "call nc " (hex-dword %))]
   0xDC [:call c address 24 3 #(str "call " (hex-dword %))]
   0xE0 [:ld <FF00+n> a 12 2 #(str "ldh [FF00+" (hex-word %) "],a")]
   0xEA [:ld <address> a 16 3 #(str "ld [" (hex-dword %) "],a")]
   0xF3 [:di 4 1 (constantly "di")]
   0xFB [:ei 4 1 (constantly "ei")]
   0xFE [:ld a <FF00+n> 12 2 #(str "ldh a,[FF00+" (hex-word %) "]")]})
