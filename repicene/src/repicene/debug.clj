(ns repicene.debug
  (:require [clojure.core.async :refer [go >! <!!]]))

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

(defn- debug-view [gameboy]
  (select-keys gameboy [:registers]))

(defmethod handle-debug-command :inspect
  [_]
  [identity debug-view])

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
  [{:keys [debug-chan] :as cpu} command]
  (let [[new-cpu response] ((apply juxt (handle-debug-command command)) cpu)
        tx-response (->response command response)]
    (println "sending" tx-response)
    (go (>! debug-chan tx-response))
    new-cpu))

(defn process-breakpoint [{:keys [debug-chan] :as cpu}]
  (loop [cpu     cpu
         command (<!! debug-chan)]
    (println "while waiting for resume, i received" command)
    (if (= :resume command)
      cpu
      (recur (process-debug-command cpu command)
             (<!! debug-chan)))))
