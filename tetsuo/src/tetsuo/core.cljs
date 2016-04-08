(ns tetsuo.core
  (:require [reagent.core :as reagent :refer [atom]]
            [tetsuo.xml :as xml]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state
         (atom
           {:posology-request
            {:patient {}}}))

(defn labeled [label input-name input]
  [:div
   [:label {:for input-name} label]
   input])

(defn input [input-name type]
  [:input {:type type :name input-name}])

(defn input-date [label input-name]
  (labeled label input-name
           (input input-name :date)))

(defn input-number [label input-name]
  (labeled label input-name
           (input input-name :number)))

(defn input-select [label input-name values]
  (labeled label input-name
           [:select {:name input-name} (map (fn [[value label]] [:option {:value value} label]) values)]))

(defn input-submit-form [label]
  [:input {:type :submit :value label}])

(println (xml/xml-request {:foo :bar}))

(reagent/render-component [:form {}
                           (input-date "date of birth" "dateOfBirth")
                           (input-select "gender" "gender" [["MALE" "Male"] ["FEMALE" "Female"]])
                           (input-number "weight" "weight")
                           (input-number "height" "height")
                           (input-select "hepathic insufficiency" "hepaticInsufficiency" [["NONE" "None"] ["SEVERE" "Severe"]])
                           (input-submit-form "GO")]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
