(ns repicene-debugger.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [>! <! chan timeout]]
            [cljs.tools.reader.edn :as edn]
            [repicene-debugger.ui :as ui])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(println "This is from src/repicene-debugger/core.cljs. Go ahead and edit it and see reloading in action.")

(defn make-ws []
  (let [ws      (js/WebSocket. "ws://localhost:2020/ws/debug")
        [ws-rx ws-tx :as chans] [(chan) (chan)]]
    (set! (.-onmessage ws) #(do (println "received" (.-data %)) (go (>! ws-rx (.-data %)))))
    (set! (.-onopen ws) #(println "connected." %))
    (set! (.-onerror ws) #(println "connection failed." %))
    (go-loop []
             (let [message (<! ws-tx)]
               (println "sending" message)
               (.send ws message))
             (recur))
    chans
    ))

(def app-state
  (let [[ws-rx ws-tx] (make-ws)]
    (atom {:ws-rx ws-rx
           :ws-tx ws-tx})))

(defn hello-world []
  [:div
   [:a {:href "#" :on-click #(go (>! (:ws-tx @app-state) ":inspect"))} "Send"]
   [:div (ui/registers (:gameboy @app-state))]])

(reagent/render-component [hello-world]
                          (. js/document (getElementById "app")))


(go-loop []
         (let [gameboy (<! (:ws-rx @app-state))
               _ (println "gameboy 0" gameboy)
               gameboy (edn/read-string gameboy)
               _ (println "gameboy 1" gameboy)
               gameboy (:response gameboy)
               _ (println "gameboy 2" gameboy)]
           (println "gameboy" gameboy)
           (swap! app-state assoc :gameboy gameboy)
           (recur)))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )