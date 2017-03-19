(ns repicene.server
  (:require [org.httpkit.server :as http-kit]
            [repicene.core :refer [demo-gameboy cpu-loop]]
            [clojure.edn :as edn]
            [clojure.core.async :refer [go go-loop >! <! thread]]))

(defn connect! [ws-channel {:keys [debug-chan] :as gameboy}]
  (println "client connected")
  (thread (try (cpu-loop gameboy)
               (catch Exception _ (do
                                    (http-kit/send! ws-channel "Gameboy crashed")
                                    (http-kit/close ws-channel)))))
  (go-loop []
    (let [response (prn-str (<! debug-chan))]
      (http-kit/send! ws-channel response)
      (recur))))

(defn disconnect! [{:keys [debug-chan]}]
  (fn [status]
    (go (>! debug-chan :kill))
    (println "client disconnected with status " status)))

(defn command-received [{:keys [debug-chan]}]
  (fn [message]
    (println "received " message)
    (let [command (edn/read-string message)]
      (go (>! debug-chan command)))))


(defn debug-handler [{:keys [uri] :as request}]
  (println uri)
  (when (clojure.string/starts-with? uri "/ws/debug")
    (let [gameboy (demo-gameboy)]
      ;; what if there is an exception?
      (http-kit/with-channel
        request channel
        (connect! channel gameboy)
        (http-kit/on-close channel (disconnect! gameboy))
        (http-kit/on-receive channel (command-received gameboy))))))

;contains function that can be used to stop http-kit server
(defonce server (atom nil))

(defn start-server [port]
  (reset! server (http-kit/run-server #'debug-handler {:port port :thread 32})))

(defn stop-server []
  (when @server
    (@server :timeout 100)
    (reset! server nil)))
