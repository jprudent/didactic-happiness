(ns tetsuo.xml
  (:require [hickory.core :as hc]
            [hickory.render :as hr]))

(defn m->hickory [m]
  (map (fn [[k v]] {:type :element :tag k :content (str v)}) m))

(defn xml-request [m]
  (hr/hickory-to-html {:type :document :content (m->hickory m)}))