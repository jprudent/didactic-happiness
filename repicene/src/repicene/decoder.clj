(ns repicene.decoder)

(defn dword? [x] (<= 0 x 0xFFFF))
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
  "returns the high word composing the unsigned dword"
  [dword]
  {:pre [(<= 0 dword 0xFFFF)]}
  (bit-shift-right dword 8))

(defn def-register [register]
  (fn
    ([cpu]
     (get-in cpu [:registers register]))
    ([cpu modifier]
     (if (fn? modifier)
       (update-in cpu [:registers register] modifier)
       (assoc-in cpu [:registers register] modifier)))))

(def pc (def-register :PC))
(def sp (def-register :SP))
(def af (def-register :AF))
(def bc (def-register :BC))
(def de (def-register :DE))
(def hl (def-register :HL))
(defn a ([cpu] (high-word (af cpu))))

(defn set-word-at [{:keys [memory] :as cpu} address val]
  {:pre [(<= 0 address 0xFFFF)]}
  (let [[index [from & _]] (lookup-backend-index memory address)
        backend-relative-address (- address from)]
    (update-in cpu [:memory index 2] assoc backend-relative-address val)))

(defn dword
  ([{:keys [memory] :as cpu}]
   (cat8 (word-at memory (+ 2 (pc cpu)))
         (word-at memory (+ 1 (pc cpu)))))
  ([cpu val]
   (set-word-at cpu (dword cpu) val)))
;; synonyms to make the code more friendly
(def address dword)
(def memory-pointer dword)

(def always (constantly true))

(def hex-dword (comp hex16 dword))

(defn fetch [{:keys [memory] :as cpu}]
  (println "fetched" (hex8 (word-at memory (pc cpu))))
  (word-at memory (pc cpu)))

(def decoder
  {0x00 [:nop 4 1 (constantly "nop")]
   0x01 [:ld16 bc dword 12 3 #(str "ld bc," (hex-dword %))]
   0x11 [:ld16 de dword 12 3 #(str "ld de," (hex-dword %))]
   0x21 [:ld16 hl dword 12 3 #(str "ld hl," (hex-dword %))]
   0x31 [:ld16 sp dword 12 3 #(str "ld sp," (hex-dword %))]
   ;;0x3E [:ld a word 8 2 #(str "ld a," (hex-word %))]
   0xC0 [:ret-cond :nz address [20 8] 3 #(str "ret nz " (hex-dword %))]
   0xC2 [:jp :nz address [16 12] 3 #(str "jp nz " (hex-dword %))]
   0xC3 [:jp always address 8 3 #(str "jp " (hex-dword %))]
   0xEA [:ld16 memory-pointer a 16 3 #(str "ld [" (hex-dword %) "],A")]
   0xF3 [:di 4 1 (constantly "di")]
   0xFB [:ei 4 1 (constantly "ei")]})
