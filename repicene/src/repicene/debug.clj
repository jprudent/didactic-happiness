(ns repicene.debug
  (:require [clojure.core.async :refer [go >! <!!]]
            [repicene.decoder :refer [decoder word-at dword? pc]]))

(defn- ->response [command response]
  {:command command :response response})

(defmulti
  handle-debug-command
  "Handle a debug command. Returns a vector of 2 functions that takes a
    gameboy as parameter. The first one will be the next state of the gameboy.
    The second one will be the response sent to the client."
  (fn [command]
    (if (sequential? command)
      (first command)
      command)))

(defn decode [{:keys [memory] :as cpu} address]
  (let [instruction (or (decoder (word-at memory address)) [1 (constantly "???")])
        to-string   (last instruction)
        size        (last (butlast instruction))]
    [(to-string cpu) size]))

(defn decode-from
  ([cpu] (decode-from cpu (pc cpu)))
  ([{:keys [memory] :as cpu} address]
   {:pre [(dword? address)]}
   (lazy-seq
     (let [cpu     (pc cpu address)
           [instr-str size] (decode cpu address)
           bytes   (map #(word-at memory (+ % (pc cpu))) (range 0 size))
           next-pc (mod (+ size address) 0x10000)]
       (cons [address bytes instr-str]
             (decode-from cpu next-pc))))))

(defn- debug-view [gameboy]
  (select-keys gameboy [:registers]))

(defmethod handle-debug-command :inspect
  [_]
  [identity #(into (debug-view %) {:instructions (take 10 (decode-from %))})])

(defmethod handle-debug-command :decode-memory
  [[_ address-start length]]
  {:pre [(dword? address-start)]}
  [identity (fn [cpu] (take length (decode-from cpu address-start)))])

(defmethod handle-debug-command :alter
  [[_ f-cpu]]
  (let [f (eval f-cpu)]
    [f f]))

(defmethod handle-debug-command :kill
  [_]
  (throw (Exception. "Harakiri")))

(defmethod handle-debug-command :default
  [_]
  [identity (constantly "J'aime faire des craquettes au chien")])

(defn process-debug-command
  [{:keys [debug-chan-tx] :as cpu} command]
  (let [[new-cpu response] ((apply juxt (handle-debug-command command)) cpu)
        tx-response (->response command response)]
    (println "sending" tx-response)
    (go (>! debug-chan-tx tx-response))
    new-cpu))

