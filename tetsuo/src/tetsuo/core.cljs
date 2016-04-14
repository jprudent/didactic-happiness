(ns tetsuo.core
  (:require [reagent.core :as reagent :refer [atom]]
            [tetsuo.xml :as xml]
            [tetsuo.client :as cli]
            [hickory.select :as s]))

(enable-console-print!)

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


(defn target-value [event] (-> event .-target .-value))

(defn element-value [element]
  (apply str (->> element
                  :content
                  (filter string?))))

(defn feed-title [element]
  (when element
    (print "yyyyyy" element)
    [:h2 (element-value element)]))

(defn str-content [entry tag]
  (for [alert (s/select (s/tag tag) entry)]
    (-> alert :content first)))

(defn li [v]
  (when v [:li v]))

(defn td [element tag]
  (let [values (str-content element tag)]
    [:td
     [:ul
      (map li values)]]))

(defn output-entry [element]
  [:tr
   (td element :vidal:indication)
   (td element :vidal:phase)
   (td element :vidal:condition)
   (td element :vidal:route)
   (td element :vidal:doseRange)
   (td element :vidal:frequencyRange)
   (td element :vidal:cumulatedMaximumDose)
   (td element :vidal:usualMaximumDoseAtOnce)
   (td element :vidal:usualMinimumDuration)
   (td element :vidal:usualMaximumDuration)
   (td element :vidal:usualMinimumAbsoluteDuration)
   (td element :vidal:usualMaximumAbsoluteDuration)
   (td element :vidal:alert)])


(defn output-scientific-tool [state]
  [:iframe {:src   (str "http://posology.vidal.net/#/" (:vmp-id @state) "/posology")
            :style {:width  "100%"
                    :height "1000px"}}])

(defmulti output-http-response (fn [response state] (:status response)))

(defn output-entries [entries]
  [:table
   [:tr
    [:td "Indications"]
    [:td "Phase"]
    [:td "Conditions"]
    [:td "Routes"]
    [:td "Dose usuelle"]
    [:td "Fréquence"]
    [:td "Dose cumulative totale"]
    [:td "Dose unitaire maximale (en une prise)"]
    [:td "Durée min usuelle"]
    [:td "Durée max usuelle"]
    [:td "Durée absolue min usuelle"]
    [:td "Durée absolue max usuelle"]
    [:td "Alertes"]]
   (map output-entry entries)]
  )

(defn output-feed [feed]
  [:div
   (feed-title (first (s/select (s/child (s/tag :feed) (s/tag :title)) feed)))
   (output-entries (s/select (s/tag :entry) feed))])

(defmethod output-http-response 200 [response state]
  [:div

   (->> response
        :body
        xml/parse
        xml/extract-feeds
        (map output-feed))
   (output-scientific-tool state)])

(defmethod output-http-response 400 [_ _]
  [:p "mauvaise requête"])

(defmethod output-http-response 500 [_ _]
  [:p "erreur serveur"])

(defmethod output-http-response 204 [_ _]
  [:p "aucun descripteur posologique correspondant"])

(defmethod output-http-response :default [r _]
  (println "no handlers for : " r))



(defn output [state]
  (output-http-response (:response @state) state))

(defn valid-input? [state]
  true)

(defn handler
  [response]
  (swap! app-state assoc-in [:response] response)
  (.log js/console (str response)))

(defn save-state! [selector event]
  (let [state (swap! app-state assoc-in selector (target-value event))]
    (when (valid-input? state)
      (cli/posology-descriptors (:vmp-id state) (xml/xml-request (:body state)) handler))))

(defn save-patient! [selector event]
  (save-state! (into [:body :posology-request :patient] selector) event))

(reagent/render-component [:div {}
                           (input-number "VMP id" "vmp-id" {:on-change (partial save-state! [:vmp-id])})
                           (input-date "date of birth" "dateOfBirth" {:on-change (partial save-patient! [:dateOfBirth])})
                           (input-select "gender" "gender" [["MALE" "Male"] ["FEMALE" "Female"]] {:on-change (partial save-patient! [:gender])})
                           (input-number "weight" "weight" {:on-change (partial save-patient! [:weight])})
                           (input-number "height" "height" {:on-change (partial save-patient! [:height])})
                           (input-select "hepathic insufficiency" "hepaticInsufficiency" [["NONE" "None"] ["SEVERE" "Severe"]] {:on-change (partial save-patient! [:hepatic-insufficiency])})
                           [output app-state]]
                          (. js/document (getElementById "app")))

(comment
  (cli/posology-descriptors 2 handler))