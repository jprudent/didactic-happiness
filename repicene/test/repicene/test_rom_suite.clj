(ns repicene.test-rom-suite
  (:require [clojure.test :refer :all]
            [repicene.schema :as s]
            [repicene.file-loader :as file-loader]
            [repicene.decoder :as decoder]
            [repicene.core :as repicene]
            [repicene.debug :as debug]
            [repicene.address-alias :as at]
            [clojure.spec :as spec]
            [clojure.core.async :refer [go <! >! chan alts!! timeout offer!]]))


;; Read roms/cpu_instrs/individual/01-special.gb

(defn kill-when-break!
  [cpu]
  (go
    (println "received " (<! (:debug-chan-tx cpu)))
    (>! (:debug-chan-rx cpu) :kill)))

(defn test-rom [path seconds]
  (let [cpu (-> (vec (take 0x8000 (file-loader/load-rom path)))
                (repicene/new-cpu)
                (decoder/pc 0x100)
                (debug/set-w-breakpoint at/serial-transfer-data
                                        (fn [cpu val]
                                          (println "SERIAL: " (char val))
                                          cpu))
                (debug/set-w-breakpoint at/serial-transfer-control
                                        (fn [cpu val]
                                          (println "SERIAL SEND")
                                          cpu))
                (debug/set-breakpoint 0xC7D2 :permanent-breakpoint))]
    (is (s/valid? cpu) (spec/explain ::s/cpu cpu))
    (kill-when-break! cpu)
    (let [response-chan (chan)]
      (go
        (time
          (try
            (repicene/cpu-loop cpu)
            (catch Exception _
              (>! response-chan true)))))
      (is (first (alts!! [response-chan (timeout (* 1000 seconds))])) path)
      (go (offer! (:debug-chan-rx cpu) :kill)))))

(deftest integration
  (testing "01-specials"
    (test-rom "roms/cpu_instrs/individual/01-special.gb" 10)
    #_(test-rom "roms/cpu_instrs/individual/03-op sp,hl.gb" 600)))