(ns repicene.main
  (:require
    [repicene.core :as gameboy]
    [repicene.server :as server]
    [clojure.tools.logging :as log]))

(defn -main [& _]
  (.addShutdownHook (Runtime/getRuntime) (Thread. server/stop-server))
  (server/start-server 2017)
  (log/info "server started on port:" 2017))
