(ns tetsuo.xml
  (:require [hickory.core :as hc]
            [hickory.render :as hr]
            [hickory.select :as s]))

(defn m->hickory [m]
  (letfn [(v->element [[k v]]
            {:type    :element
             :tag     k
             :content (if (map? v) (m->hickory v) (str v))})]
    (map v->element m)))

(defn xml-request [m]
  (hr/hickory-to-html {:type :document :content (m->hickory m)}))

(defn extract-feeds [hick]
  (s/select (s/tag :feed) hick))

(defn parse [s]
  (hc/as-hickory (hc/parse s)))
