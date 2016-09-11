(ns cheap-hate.demo
  (:require [cheap-hate.simple-machine :as machine]
            [cheap-hate.instructions :as instructions]
            [cheap-hate.console-screen :refer [->ConsoleScreen]]
            [cheap-hate.console-keyboard :refer [->ConsoleKeyboard]]
            [cheap-hate.romloader :as rom]
            [cheap-hate.core :as core]))

(instructions/start-machine
  {:fresh-machine   machine/fresh-machine
   :screen          #_(reify core/Screen (print-screen [_ _ _])) (->ConsoleScreen machine/fresh-machine)
   :program         (rom/load-rom "roms/BRIX")
   :keyboard        (->ConsoleKeyboard)
   :flight-recorder (reify core/FlightRecorder (record [_ _ _]))})