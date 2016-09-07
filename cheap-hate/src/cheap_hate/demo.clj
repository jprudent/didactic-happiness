(ns cheap-hate.demo
  (:require [cheap-hate.core :as c8]
            [cheap-hate.console-screen :refer :all]
            [cheap-hate.romloader :as rom]))

(c8/start-machine (->ConsoleScreen) (rom/load-rom "roms/BRIX"))