(ns repicene-debugger.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "This is printed from src/repicene-debugger/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defn make-ws []
  (let [ws (js/WebSocket. "ws://localhost:2020/ws/debug")]
    (set! (.-onmessage ws) println)))

(defonce app-state (atom {:ws (make-ws)}))

(defn send! [ws]
  (.send ws ":foo"))

(send! (:ws app-state))

(println "foooo")

(defn hello-world []
  [:h1 (:text @app-state)])

(reagent/render-component [hello-world]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
