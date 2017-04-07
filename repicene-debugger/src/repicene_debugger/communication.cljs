(ns repicene-debugger.communication
  (:require [cljs.core.async :refer [>! <! chan timeout]]
            [cljs.tools.reader.edn :as edn])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defn make-ws
  "Open a websocket to specified address. Returns a vector of two async chans
  that are plugged on the websocket.
  The first one for rx (read) from socket,
  the second one for tx (write) to socket"
  []
  (let [ws (js/WebSocket. "ws://localhost:2020/ws/debug")
        [ws-rx ws-tx :as chans] [(chan) (chan)]]
    (set! (.-onmessage ws) #(do (println "received" (.-data %))
                                (go (>! ws-rx (edn/read-string (.-data %))))))
    (set! (.-onopen ws) #(println "connected." %))
    (set! (.-onerror ws) #(println "connection failed." %))
    (go-loop []
             (let [message (<! ws-tx)]
               (println "sending" message)
               (.send ws (prn-str message)))
             (recur))
    chans))

(defonce communication (atom (make-ws)))
(defonce rx (first @communication))
(defonce tx (second @communication))