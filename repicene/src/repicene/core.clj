(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]))

(defn word-at [memory address]
  (some (fn [[from to backend]]
          (when (<= from address to)
            (let [backend-relative-address (- address from)]
              (get backend backend-relative-address))))
        memory))

(defn new-cpu [rom]
  {:registers {:AF 0
               :BC 0
               :DE 0
               :HL 0
               :SP 0
               :PC 0}
   :memory    [[0 0x7FFF rom]]})


(defn fetch [{:keys [memory registers] :as cpu}]
  (let [pc (:PC registers)]
    (map #(word-at memory (+ %1 pc)) [0 1 2])))

(defn decode [[opcode1 opcode2]]
  (condp = opcode1
    0 [:nop]))

(defmulti exec (fn [_ [instr & _]] instr))
(defmethod exec :nop [cpu _] cpu)

(defn cpu-loop [cpu]
  (let [[opcode1 opcode2 :as opcodes] (fetch cpu)
        instr (decode opcodes)
        cpu (exec cpu instr)]))