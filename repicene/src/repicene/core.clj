(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]
            [clojure.core.async :as async :refer [go >! chan poll! <!! thread]]))

;; a word is an 8 bits positive integer
;; a dword is a 16 bits positive integer

(def hex8 (partial format "0x%02X"))
(def hex16 (partial format "0x%04X"))
(defn cat8
  "concatenate two words to make a double"
  [x y]
  {:pre [(<= 0 x 255) (<= 0 y 255)]}
  (bit-or (bit-shift-left x 8) y))

(defn high-word
  "returns the high word composing the unsigned dword"
  [dword]
  {:pre [(<= 0 dword 0xFFFF)]}
  (bit-shift-right dword 8))

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

(defn set-word-at [{:keys [memory] :as cpu} address val]
  {:pre [(<= 0 address 0xFFFF)]}
  (let [[index [from & _]] (lookup-backend-index memory address)
        backend-relative-address (- address from)]
    (update-in cpu [:memory index 2] assoc backend-relative-address val)))

(defn new-cpu [rom]
  (let [wram-1 (vec (take 0x1000 (repeat 0)))]
    {:registers          {:AF 0
                          :BC 0
                          :DE 0
                          :HL 0
                          :SP 0
                          :PC 0}
     :interrupt-enabled? true
     :memory             [[0x0000 0x7FFF rom]
                          [0xD000 0xDFFF wram-1]]
     :debug-chan         (chan)
     :x-breakpoints      []}))

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
  {0x00 [:nop 4 (constantly "nop")]
   0x01 [:ld16 bc dword 12 #(str "ld bc," (hex-dword %))]
   0x11 [:ld16 de dword 12 #(str "ld de," (hex-dword %))]
   0x21 [:ld16 hl dword 12 #(str "ld hl," (hex-dword %))]
   0x31 [:ld16 sp dword 12 #(str "ld sp," (hex-dword %))]
   0xC0 [:ret-cond :nz address [20 8] #(str "ret nz " (hex-dword %))]
   0xC2 [:jp :nz address [16 12] #(str "jp nz " (hex-dword %))]
   0xC3 [:jp always address 8 #(str "jp " (hex-dword %))]
   0xEA [:ld16 memory-pointer a 16 #(str "ld [" (hex-dword %) "],A")]
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

(defn x-bp? [{:keys [x-breakpoints] :as cpu}]
  (some (partial = (pc cpu)) x-breakpoints))

(defn ->response [command response]
  {:command command :response response})

(defmulti handle-debug-command
          (fn [command]
            (if (sequential? command)
              (first command)
              command)))

(defmethod handle-debug-command :inspect
  [_]
  [identity identity])

(defmethod handle-debug-command :alter
  [[_ f-cpu]]
  (let [f (eval f-cpu)]
    [f f]))

(defmethod handle-debug-command :default
  [_]
  [identity (constantly "J'aime faire des craquettes au chien")])

(defn process-debug-command
  [{:keys [debug-chan] :as cpu} command]
  (let [[new-cpu response] ((apply juxt (handle-debug-command command)) cpu)]
    (go (>! debug-chan (->response command response)))
    new-cpu))

(defn process-breakpoint [{:keys [debug-chan] :as cpu}]
  (loop [cpu     cpu
         command (<!! debug-chan)]
    (if (= :resume command)
      cpu
      (recur (process-debug-command cpu command)
             (<!! debug-chan)))))

(defn cpu-cycle [cpu]
  (let [instr (get decoder (fetch cpu))]
    (println (str "@" (hex16 (pc cpu))) ((last instr) cpu))
    (exec cpu instr)))

(defn cpu-loop [{:keys [debug-chan] :as cpu}]
  (let [command (poll! debug-chan)]
    (recur
      (cond-> cpu
              command (process-debug-command command)
              (x-bp? cpu) (process-breakpoint)
              :always (cpu-cycle)))))

#_(def cpu
    (->
      (load-rom "roms/cpu_instrs/cpu_instrs.gb")
      (new-cpu)
      (assoc-in [:registers :PC] 0x100)))

;; POC BREAKPOINT
#_(do
    (def cpu
      (->
        (load-rom "roms/cpu_instrs/cpu_instrs.gb")
        (new-cpu)
        (assoc-in [:registers :PC] 0x100)
        (update-in [:x-breakpoints] conj 0x637)))
    (thread (cpu-loop cpu))
    (async/>!! (:debug-chan cpu) "yolo"))