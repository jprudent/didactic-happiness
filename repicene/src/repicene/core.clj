(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]))

;; a word is an 8 bits positive integer
;; a dword is a 16 bits positive integer

(def hex8 (partial format "0x%02X"))
(def hex16 (partial format "0x%04X"))
(defn cat8
  "concatenate two words to make a double"
  [x y]
  {:pre [(<= 0 x 255) (<= 0 y 255)]}
  (bit-or (bit-shift-left x 8) y))

(defn word-at [memory address]
  (some (fn [[from to backend]]
          (when (<= from address to)
            (let [backend-relative-address (- address from)]
              (get backend backend-relative-address))))
        memory))

(defn new-cpu [rom]
  {:registers          {:AF 0
                        :BC 0
                        :DE 0
                        :HL 0
                        :SP 0
                        :PC 0}
   :interrupt-enabled? true
   :memory             [[0 0x7FFF rom]]})

(defn def-register [register]
  (fn
    ([cpu] (get-in cpu [:registers register]))
    ([cpu modifier]
     (if (fn? modifier)
       (update-in cpu [:registers register] modifier)
       (assoc-in cpu [:registers register] modifier)))))

(def pc (def-register :PC))
(def sp (def-register :SP))
(def bc (def-register :BC))
(def de (def-register :DE))
(def hl (def-register :HL))

(defn dword [{:keys [memory] :as cpu}]
  (cat8 (word-at memory (+ 2 (pc cpu)))
        (word-at memory (+ 1 (pc cpu)))))

(def always (constantly true))

(def hex-dword (comp hex16 dword))

(defn fetch [{:keys [memory] :as cpu}]
  (println "fetched" (hex8 (word-at memory (pc cpu))))
  (word-at memory (pc cpu)))

(def decoder
  {0x00 [:nop 4 (constantly "nop")]
   0x01 [:ld16 bc dword 12 #(str "ld bc," (hex-dword %))]
   0x11 [:ld16 de dword 12 #(str "ld de," (hex-dword %))]
   0x01 [:ld16 hl dword 12 #(str "ld hl," (hex-dword %))]
   0x31 [:ld16 sp dword 12 #(str "ld sp," (hex-dword %))]
   0xC0 [:ret-cond :nz dword [20 8] #(str "ret nz " (hex-dword %))]
   0xC2 [:jp :nz dword [16 12] #(str "jp nz " (hex-dword %))]
   0xC3 [:jp always dword 8 #(str "jp " (hex-dword %))]
   ;;0xEA [:ld8 dword a 16 #(str "ld [" (hex-dword %) "],A")]
   0xF3 [:di 4 (constantly "di")]
   0xFB [:ei 4 (constantly "ei")]})

(defmulti exec (fn [_ [instr & _]] instr))
(defmethod exec :nop [cpu _] (pc cpu inc))
(defmethod exec :jp [cpu [_ condition address & _]]
  (if (condition cpu)
    (pc cpu (address cpu))
    (pc cpu (partial + 3))))
(defmethod exec :di [cpu _]
  (-> (assoc cpu :interrupt-enabled? false)
      (pc inc)))
(defmethod exec :ei [cpu _]
  (-> (assoc cpu :interrupt-enabled? true)
      (pc inc)))
(defmethod exec :ld16 [cpu [_ destination source]]
  (-> (destination cpu (source cpu))
      (pc (partial + 3))))

(defn cpu-loop [cpu]
  (let [instr (get decoder (fetch cpu))]
    (println (str "@" (hex16 (pc cpu))) ((last instr) cpu))
    (recur (exec cpu instr))))

#_(def cpu
    (->
      (load-rom "roms/cpu_instrs/cpu_instrs.gb")
      (new-cpu)
      (assoc-in [:registers :PC] 0x100)))