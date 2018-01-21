(ns repicene.cpu-protocol
  (:require [clojure.core.async :as async]
            [repicene.schema :as s]))

(def ^short cell 0)

(defprotocol UpdatableCpu
  (set-word-at [this address val])
  (word-at [this address])
  (set-dword-register [this register word])
  (dword-register [this register]))

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


(defrecord Cpu [registers
                interrupt-enabled?
                memory
                mode
                x-breakpoints
                debug-chan-rx
                debug-chan-tx
                history-chan
                serial-sent-chan]
  UpdatableCpu
  (set-dword-register [this register modifier]
    (assoc this :registers
                (persistent! (assoc! (transient registers) register
                                     (if (fn? modifier) (modifier (register registers)) modifier)))))

  (dword-register [_ register]
    (register registers))

  (set-word-at [this address word]
    (-> (assoc this :memory (assoc memory address word))
        (io-update address word)))

  (word-at [_ address]
    (nth memory address)))

(defn new-cpu [rom]
  {:pre [(= 0x8000 (count rom))]}
  (map->Cpu {:registers          {:AF 0
                                  :BC 0
                                  :DE 0
                                  :HL 0
                                  :SP 0
                                  :PC 0}
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
             :debug-chan-rx      (async/chan)
             :debug-chan-tx      (async/chan)
             :history-chan       (async/chan (async/sliding-buffer 100))
             :serial-sent-chan   (async/chan (async/sliding-buffer 100))
             :x-breakpoints      {}}))