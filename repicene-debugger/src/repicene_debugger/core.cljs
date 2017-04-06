(ns repicene-debugger.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [>! <! chan timeout]]
            [cljs.tools.reader.edn :as edn]
            [repicene-debugger.ui :as ui]
            [repicene.schema :as s])
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

(let [[rx tx] (make-ws)]
  (def ws-rx rx)
  (def ws-tx tx))

(defonce app-state
         (atom {}))

(defn inspect-params []
  [:inspect {:regions [[0xDF00 0xDFFF]]}])

(defn pc []
  (get-in @app-state [:gameboy ::s/registers ::s/PC]))

(defn do-step-into []
  (go (>! ws-tx :step-into)))

(defn do-step-over []
  (go (>! ws-tx :step-over)))

(defn do-back-step []
  (go (>! ws-tx :back-step)))

(defn do-reset []
  (go (>! ws-tx :reset)))

(defn hello-world []
  [:div
   [:a {:href "#" :on-click #(go (>! ws-tx (inspect-params)))} "Inspect"]
   [:div.debugger
    (ui/registers (:gameboy @app-state))
    (ui/instructions (:gameboy @app-state) (pc))
    (ui/memory (:gameboy @app-state))
    [:div
     (into ui/empty-button [{:on-click #(go (>! ws-tx :resume))} "Resume"])
     (into ui/empty-button [{:on-click do-step-into} "Step into"])
     (into ui/empty-button [{:on-click do-step-over} "Step over"])
     (into ui/empty-button [{:on-click do-back-step} "Back step"])
     (into ui/empty-button [{:on-click do-reset} "Reset"])]]])

(reagent/render-component [hello-world]
                          (. js/document (getElementById "app")))


(defmulti response-handler
          (fn [{:keys [command]}]
            (if (sequential? command)
              (first command)
              command)))

(defmethod response-handler :inspect
  [{:keys [response]}]
  (swap! app-state assoc :gameboy response))

(defmethod response-handler :step-into
  [{:keys [response]}]
  (swap! app-state assoc :gameboy response))

(defmethod response-handler :step-over
  [{:keys [response]}]
  (when (not= :running response)
    (swap! app-state assoc :gameboy response)))

(defmethod response-handler :back-step
  [{:keys [response]}]
  (swap! app-state assoc :gameboy response))


(defmethod response-handler :break
  [_]
  (go (>! ws-tx (inspect-params))))

(defmethod response-handler :reset
  [_]
  (go (>! ws-tx (inspect-params))))

(defmethod response-handler :default
  [response]
  (println "Error! Unhandled response'" response "'"))

(go-loop []
         (response-handler (<! ws-rx))
         (recur))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )