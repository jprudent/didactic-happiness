(ns cheap-hate.demo
  (:require [cheap-hate.simple-machine :as machine]
            [cheap-hate.instructions :as instructions]
            [cheap-hate.console-screen :refer :all]
            [cheap-hate.romloader :as rom]
            [cheap-hate.core :as core]))

(defrecord NilKeyboard []
  core/Keyboard
  (pressed-key [_] nil))

(defrecord MuteFlightRecorder []
  core/FlightRecorder
  (record [_ _ _]))

(instructions/start-machine
  {:fresh-machine   machine/fresh-machine
   :screen          (->ConsoleScreen)
   :program         (rom/load-rom "roms/BRIX")
   :keyboard        (reify core/Keyboard (pressed-key [_]))
   :flight-recorder (reify core/FlightRecorder (record [_ _ _]))})