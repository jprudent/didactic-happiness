(ns repicene.launch
  (:require [repicene.server :refer [start-server]])
  (:gen-class))

(defn -main
  [& args]
  (start-server 2020))