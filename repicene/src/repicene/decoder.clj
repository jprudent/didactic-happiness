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
(def b (def-word-register high-word bc))
(def c (def-word-register low-word bc))
(def d (def-word-register high-word de))
(def e (def-word-register low-word de))
(def h (def-word-register high-word hl))
(def l (def-word-register low-word hl))

(defn set-word-at [{:keys [memory] :as cpu} address val]
  {:pre [(dword? address) (word? val)]}
  (let [[index [from & _]] (lookup-backend-index memory address)
        backend-relative-address (- address from)]
    (update-in cpu [:memory index 2] assoc backend-relative-address val)))

(defn dword
  ([{:keys [memory] :as cpu}]
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
   0xC0 [:ret-cond :nz address [20 8] 3 #(str "ret nz " (hex-dword %))]
   0xC2 [:jp :nz address [16 12] 3 #(str "jp nz " (hex-dword %))]
   0xC3 [:jp always address 8 3 #(str "jp " (hex-dword %))]
   0xE0 [:ld <FF00+n> a 12 2 #(str "ldh [FF00+" (hex-word %) "],a")]
   0xEA [:ld <address> a 16 3 #(str "ld [" (hex-dword %) "],a")]
   0xF3 [:di 4 1 (constantly "di")]
   0xFB [:ei 4 1 (constantly "ei")]
   0xFE [:ld a <FF00+n> 12 2 #(str "ldh a,[FF00+" (hex-word %) "]")]})
