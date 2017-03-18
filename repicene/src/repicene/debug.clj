(ns repicene.debug
  (:require [clojure.core.async :refer [go >! <!!]]))

(defn- ->response [command response]
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
