(ns repicene-debugger.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    [cljs.core.async :refer [>! <! chan timeout]]
    [repicene-debugger.ui :as ui]
    [repicene.schema :as s]
    [repicene-debugger.communication :refer [rx]]
    [repicene-debugger.command :as cmd])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(enable-console-print!)

(defonce app-state (atom {}))

(defn pc []
  (get-in @app-state [:gameboy ::s/registers ::s/PC]))

(defn hello-world []
  [:div
   [:a {:href "#" :on-click cmd/do-inspect} "Inspect"]
   [:div.debugger
    (ui/registers (:gameboy @app-state))
    (ui/instructions (:gameboy @app-state) (pc))
    (ui/memory (:gameboy @app-state))
    [:div
     (into ui/empty-button [{:on-click cmd/do-resume} "Resume"])
     (into ui/empty-button [{:on-click cmd/do-step-into} "Step into"])
     (into ui/empty-button [{:on-click cmd/do-step-over} "Step over"])
     (into ui/empty-button [{:on-click cmd/do-back-step} "Back step"])
     (into ui/empty-button [{:on-click cmd/do-reset} "Reset"])]]])

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
  (cmd/do-inspect))

(defmethod response-handler :reset
  [_]
  (cmd/do-inspect))

(defmethod response-handler :default
  [response]
  (println "Error! Unhandled response'" response "'"))

(go-loop []
         (response-handler (<! rx))
         (recur))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )