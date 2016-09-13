(ns cheap-hate.main
  (:require [cheap-hate.simple-machine :as machine]
            [cheap-hate.console-screen :refer [->ConsoleScreen]]
            [cheap-hate.console-keyboard :refer :all]
            [cheap-hate.looping-clock :refer [->LoopingClock]]
            [cheap-hate.romloader :as rom]
            [cheap-hate.core :as core])
  (:gen-class))

(def ^:private layout {"azerty"  fr-layout
                       "qwerty" us-layout})
(defn -main
  [kb-layout rom]
  (core/start-machine
    machine/fresh-machine
    {:screen          (->ConsoleScreen machine/fresh-machine)
     :program         (rom/load-rom rom)
     :keyboard        (->ConsoleKeyboard (get layout kb-layout us-layout) no-key)
     :flight-recorder (reify core/FlightRecorder (record [_ _ _]))
     :clock           (->LoopingClock 600 0)}))

