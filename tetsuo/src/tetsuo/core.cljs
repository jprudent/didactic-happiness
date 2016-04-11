(ns tetsuo.core
  (:require [reagent.core :as reagent :refer [atom]]
            [tetsuo.xml :as xml]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {}))

(defn labeled [label input-name input]
  [:div
   [:label {:for input-name} label]
   input])

(defn input [input-name type attrs]
  [:input (merge {:type type :name input-name} attrs)])

(defn input-date [label input-name attrs]
  (labeled label input-name
           (input input-name :date attrs)))

(defn input-number [label input-name attrs]
  (labeled label input-name
           (input input-name :number attrs)))

(defn input-option [[value label]] [:option {:value value} label])

(defn input-select [label input-name options attrs]
  (labeled label input-name
           [:select
            (merge {:name input-name} attrs)
            (map input-option options)]))

(defn input-button [label attrs]
  [:button attrs label])

(println (xml/xml-request {:foo :bar}))

(defn ev [e] (-> e .-target .-value))

(defn save-request! [selector event]
  (swap! app-state assoc-in selector (ev event))
  (println @app-state))

(reagent/render-component [:div {}
                           (input-date "date of birth" "dateOfBirth" {:on-change (partial save-request! [:posology-request :patient :date-of-birth])})
                           (input-select "gender" "gender" [["MALE" "Male"] ["FEMALE" "Female"]] {:on-change (partial save-request! [:posology-request :patient :gender])})
                           (input-number "weight" "weight" {:on-change (partial save-request! [:posology-request :patient :weight])})
                           (input-number "height" "height" {:on-change (partial save-request! [:posology-request :patient :height])})
                           (input-select "hepathic insufficiency" "hepaticInsufficiency" [["NONE" "None"] ["SEVERE" "Severe"]] {:on-change (partial save-request! [:posology-request :patient :hepatic-insufficiency])})
                           (input-button "GO" {:on-click (fn [_] (println (xml/xml-request @app-state)))})]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
