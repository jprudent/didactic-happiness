(ns repicene.main
  (:require
    [repicene.core :as gameboy]
    [repicene.server :as server]))

(defn -main [& _]
  (.addShutdownHook (Runtime/getRuntime) (Thread. server/stop-server))
  (server/start-server 2017))
