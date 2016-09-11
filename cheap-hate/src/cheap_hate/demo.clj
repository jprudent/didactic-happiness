(ns cheap-hate.demo
  (:require [cheap-hate.simple-machine :as machine]
            [cheap-hate.instructions :as instructions]
            [cheap-hate.console-screen :refer [->ConsoleScreen]]
            [cheap-hate.console-keyboard :refer :all]
            [cheap-hate.romloader :as rom]
            [cheap-hate.core :as core]))

(instructions/start-machine
  {:fresh-machine   machine/fresh-machine
   :screen          (->ConsoleScreen machine/fresh-machine)
   :program         (rom/load-rom "roms/BRIX")
   :keyboard        (->ConsoleKeyboard fr-layout no-key)
   :flight-recorder (reify core/FlightRecorder (record [_ _ _]))})