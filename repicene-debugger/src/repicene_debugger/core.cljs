(ns repicene-debugger.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [>! <! chan timeout]]
            [cljs.tools.reader.edn :as edn]
            [repicene-debugger.ui :as ui])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(println "This is from src/repicene-debugger/core.cljs. Go ahead and edit it and see reloading in action.")

(defn make-ws
  "Open a websocket to specified address. Returns a vector of two async chans
  that are plugged on the websocket.
  The first one for rx (read), the second one for tx (write)"
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

(defonce app-state
  (let [[ws-rx ws-tx] (make-ws)]
    (atom {:ws-rx ws-rx
           :ws-tx ws-tx})))

(defn hello-world []
  [:div
   [:a {:href     "#"
        :on-click #(go (>! (:ws-tx @app-state) :inspect)
                       (>! (:ws-tx @app-state) [:decode-memory (or (get-in (:gameboy @app-state) [:registers :PC]) 0x100) 10])
                       )}
    "Send"]
   [:div.debugger
    (ui/registers (:gameboy @app-state))
    (ui/instructions (:instructions @app-state) (get-in @app-state [:gameboy :registers :PC]))]])

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

(defmethod response-handler :decode-memory
  [{:keys [response]}]
  (swap! app-state assoc :instructions response))

(defmethod response-handler :default
  [{:keys [command response]}]
  (println "Error! Unhandled response" response "for command" command))

(go-loop []
         (response-handler (<! (:ws-rx @app-state)))
         (recur))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )