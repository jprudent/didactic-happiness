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

(defmulti feed-element-handler :tag)

(defn element-value [element]
  (apply str (->> element
                  :content
                  (filter string?))))

(defmethod feed-element-handler :default [element]
  [:div (str (:tag element)) " : " (element-value element)])

(defmethod feed-element-handler :link [element]
  [:div [:a {:href (get-in element [:attrs :href] "#")}
         (str (element-value element) (get-in element [:attrs :rel] ""))]])

(defmethod feed-element-handler :title [element]
  [:h2 (element-value element)])

(defn str-content [entry tag]
  (for [alert (s/select (s/tag tag) entry)]
    (-> alert :content first)))

(defn li [v]
  (when v [:li v]))

(defn item [label v]
  (li (str label v)))

(defn ul [label element tag]
  (let [values (str-content element tag)]
    (when (not (empty? values))
      [:p label
       [:ul
        (map li values)]])))

(defn entry-element-handler [element]
  [:div
   [:h3 (str-content element :title)]
   (ul "Dose usuelle : " element :vidal:doseRange)
   (ul "Fréquence : " element :vidal:frequencyRange)
   (ul "Dose cumulative totale : " element :vidal:cumulatedMaximumDose)
   (ul "Dose unitaire maximale (en une prise) : " element :vidal:usualMaximumDoseAtOnce)
   (ul "Durée min usuelle : " element :vidal:usualMinimumDuration)
   (ul "Durée max usuelle : " element :vidal:usualMaximumDuration)
   (ul "Durée absolue min usuelle : " element :vidal:usualMinimumAbsoluteDuration)
   (ul "Durée absolue max usuelle : " element :vidal:usualMaximumAbsoluteDuration)
   (ul "Indications : " element :vidal:indication)
   (ul "Voies : " element :vidal:route)
   (ul "Alertes : " element :vidal:alert)])

(defmethod feed-element-handler :entry [element]
  (println "entry")
  [:div (entry-element-handler element)])

(defn output-scientific-tool [vmp-id]
  [:iframe {:src   (str "http://posology.vidal.net/#/" vmp-id "/posology")
            :style {:width "100%"
                    :height "1000px"}}])

(defmulti output-http-response :status)

(defmethod output-http-response 200 [response]
  [:div
   (->> response
        :body
        xml/parse
        xml/extract-feeds
        (map #(xml/walk-feed % feed-element-handler)))
   (output-scientific-tool 46)])

(defmethod output-http-response 400 [_]
  [:p "mauvaise requête"])

(defmethod output-http-response 500 [_]
  [:p "erreur serveur"])

(defmethod output-http-response 204 [_]
  [:p "aucun descripteur posologique correspondant"])

(defmethod output-http-response :default [r]
  (println "no handlers for : " r))



(defn output [state]
  (output-http-response (:response @state)))

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