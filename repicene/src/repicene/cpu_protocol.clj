(ns repicene.cpu-protocol
  (:require [clojure.core.async :as async]
            [repicene.schema :as s]))

(def ^short cell 0)

(defprotocol UpdatableCpu
  (set-word-at [this address val])
  (word-at [this address])
  (set-dword-register [this register word])
  (dword-register [this register])
  (halted? [this])
  (break? [this])
  (running? [this])
  (get-pc [this])
  (update-pc [this f])
  (set-pc [this v]))

(defrecord Registers [AF BC DE HL SP PC])

(defn io-update
  [{:keys [serial-sent-chan] :as cpu} ^long address val]
  {:post [(s/cpu? %)]}
  (case address
    0xFF01
    (do
      (async/put! serial-sent-chan val)
      cpu)
    cpu))


(defrecord Cpu [^int AF
                ^int BC
                ^int DE
                ^int HL
                ^int SP
                ^int PC
                interrupt-enabled?
                memory
                mode
                ^long clock
                x-breakpoints
                debug-chan-rx
                debug-chan-tx
                history-chan
                serial-sent-chan]
  UpdatableCpu
  (set-dword-register [this register modifier]
    (if (fn? modifier)
      (update this register modifier)
      (assoc this register modifier)))

  (dword-register [this register]
    (get this register))

  (set-word-at [this address word]
    (-> (assoc this :memory (assoc memory address word))
        (io-update address word)))

  (word-at [_ address]
    (nth memory address))

  (halted? [_] (= ::s/halted mode))
  (break? [_] (= ::s/break mode))
  (running? [_] (= ::s/running mode))

  (get-pc [_] PC)
  (update-pc [this f] (update this :PC f))
  (set-pc [this v] (assoc this :PC v)))

(defn new-cpu [rom]
  {:pre [(= 0x8000 (count rom))]}
  (map->Cpu {:AF                 (int 0)
             :BC                 (int 0)
             :DE                 (int 0)
             :HL                 (int 0)
             :SP                 (int 0)
             :PC                 (int 0)
             :interrupt-enabled? true
             :memory             (vec (concat rom                               ;; rom
                                              (repeat 0x2000 cell)              ;; vram
                                              (repeat 0x2000 cell)              ;; ext-ram
                                              (repeat 0x1000 cell)              ;; wram-0
                                              (repeat 0x1000 cell)              ;; wram-1
                                              (repeat 0x1E00 cell)              ;; echo
                                              (repeat 0x00A0 cell)              ;; oam-ram
                                              (repeat 0x0060 cell)              ;; unusable
                                              (repeat 0x0080 cell)              ;; io
                                              (repeat 0x0080 cell)))            ;; hram
             :mode               ::s/running
             :clock              0
             :debug-chan-rx      (async/chan)
             :debug-chan-tx      (async/chan)
             :history-chan       (async/chan (async/sliding-buffer 100))
             :serial-sent-chan   (async/chan (async/sliding-buffer 100))
             :x-breakpoints      {}}))