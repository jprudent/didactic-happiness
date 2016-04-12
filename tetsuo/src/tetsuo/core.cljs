(ns tetsuo.core
  (:require [reagent.core :as r]
            [tetsuo.xml :as xml]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (r/atom {}))

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

(defn ev [e] (-> e .-target .-value))

(defn save-request! [selector event]
  (swap! app-state assoc-in selector (ev event)))

(defn mockr []
  (swap! app-state assoc-in [:response] "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:vidal=\"http://api.vidal.net/-/spec/vidal-api/1.0/\">\n    <title>Product 15070</title>\n    <link rel=\"self\" type=\"application/atom+xml\" href=\"/rest/api/product/15070\" />\n    <id>vidal://product/15070</id>\n    <updated>2013-02-21T00:00:00Z</updated>\n    <dc:date>2013-02-21T00:00:00Z</dc:date>\n    <entry>\n        <title>SINTROM 4 mg cp quadriséc</title>\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/packages\" title=\"PACKAGES\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/molecules\" title=\"MOLECULES\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/molecules/active-excipients\" title=\"ACTIVE_EXCIPIENTS\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/recos\" title=\"RECOS\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/foreign-products\" title=\"FOREIGN_PRODUCTS\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/indications\" title=\"INDICATIONS\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/documents\" title=\"DOCUMENTS\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/documents/opt\" title=\"OPT_DOCUMENT\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"http://www.eurekasante.fr/medicaments/fromwidget.html?idvdf=MSINTR01.htm\" title=\"EUREKA_SANTE\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/atc-classification\" title=\"ATC_CLASSIFICATION\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/smr-asmr\" title=\"SMR_ASMR\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/smr-asmr.htm\" title=\"SMR_ASMR_HTML\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/ald\" title=\"ALD_DETAIL\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product/15070/vidal-classification\" title=\"VIDAL_CLASSIFICATION\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product-range/9389\" title=\"PRODUCT_RANGE\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/product-range/9389/products\" title=\"RANGE_PRODUCTS\" />\n        <link rel=\"related\" type=\"application/atom+xml\" href=\"/rest/api/vmp/627\" title=\"VMP\" />\n        <category term=\"PRODUCT\" />\n        <author>\n            <name>VIDAL</name>\n        </author>\n        <id>vidal://product/15070</id>\n        <updated>2013-02-21T00:00:00Z</updated>\n        <summary type=\"text\">SINTROM 4 mg cp quadriséc</summary>\n        <vidal:activePrinciples>acénocoumarol</vidal:activePrinciples>\n        <vidal:ammType vidalId=\"20\">AMM Française</vidal:ammType>\n        <vidal:beCareful>false</vidal:beCareful>\n        <vidal:bestDocType name=\"MONO\">MONO</vidal:bestDocType>\n        <vidal:cis>61510352</vidal:cis>\n        <vidal:company vidalId=\"616\" type=\"OWNER\">Novartis Pharma SAS</vidal:company>\n        <vidal:dispensationPlace name=\"PHARMACY\">PHARMACY</vidal:dispensationPlace>\n        <vidal:drugInSport>false</vidal:drugInSport>\n        <vidal:exceptional>false</vidal:exceptional>\n        <vidal:galenicForm vidalId=\"135\">comprimé quadrisécable</vidal:galenicForm>\n        <vidal:hasPublishedDoc>true</vidal:hasPublishedDoc>\n        <vidal:horsGHS>false</vidal:horsGHS>\n        <vidal:id>15070</vidal:id>\n        <vidal:itemType name=\"VIDAL\">VIDAL</vidal:itemType>\n        <vidal:list name=\"I\">Liste 1</vidal:list>\n        <vidal:marketStatus name=\"AVAILABLE\">Commercialisé</vidal:marketStatus>\n        <vidal:midwife>false</vidal:midwife>\n        <vidal:name>SINTROM 4 mg cp quadriséc</vidal:name>\n        <vidal:onMarketDate format=\"yyyy-MM-dd\">1959-09-15</vidal:onMarketDate>\n        <vidal:perVolume>4mg</vidal:perVolume>\n        <vidal:refundRate name=\"_65\">65%</vidal:refundRate>\n        <vidal:retrocession>false</vidal:retrocession>\n        <vidal:safetyAlert>true</vidal:safetyAlert>\n        <vidal:vmp vidalId=\"627\">acénocoumarol * 4 mg ; voie orale ; cp</vidal:vmp>\n        <vidal:withoutPrescription>false</vidal:withoutPrescription>\n    </entry>\n</feed>\n\n"))

(mockr)

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

(defn output-xml [state]
  (println (->> @state :response xml/parse
                xml/extract-feeds
                #_(map #(xml/walk-feed % xml/feed-element-handler))))
  [:div (->> @state :response xml/parse
                        xml/extract-feeds
                        (map #(xml/walk-feed % feed-element-handler)))])

(r/render-component [:div {}
                     (input-date "date of birth" "dateOfBirth" {:on-change (partial save-request! [:posology-request :patient :date-of-birth])})
                     (input-select "gender" "gender" [["MALE" "Male"] ["FEMALE" "Female"]] {:on-change (partial save-request! [:posology-request :patient :gender])})
                     (input-number "weight" "weight" {:on-change (partial save-request! [:posology-request :patient :weight])})
                     (input-number "height" "height" {:on-change (partial save-request! [:posology-request :patient :height])})
                     (input-select "hepathic insufficiency" "hepaticInsufficiency" [["NONE" "None"] ["SEVERE" "Severe"]] {:on-change (partial save-request! [:posology-request :patient :hepatic-insufficiency])})
                     (input-button "GO" {:on-click (fn [_] (println (xml/xml-request @app-state)))})
                     [output-xml app-state]]
                    (. js/document (getElementById "app")))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
