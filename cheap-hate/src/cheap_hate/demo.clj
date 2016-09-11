(ns cheap-hate.demo
  (:require [cheap-hate.simple-machine :as machine]
            [cheap-hate.console-screen :refer [->ConsoleScreen]]
            [cheap-hate.console-keyboard :refer :all]
            [cheap-hate.looping-quartz :refer [->LoopingQuartz]]
            [cheap-hate.romloader :as rom]
            [cheap-hate.core :as core]))

(core/start-machine
  machine/fresh-machine
  {:screen          (->ConsoleScreen machine/fresh-machine)
   :program         (rom/load-rom "roms/BRIX")
   :keyboard        (->ConsoleKeyboard fr-layout no-key)
   :flight-recorder (reify core/FlightRecorder (record [_ _ _]))
   :quartz          (->LoopingQuartz 600 0)})