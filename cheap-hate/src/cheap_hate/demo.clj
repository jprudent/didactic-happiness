(ns cheap-hate.demo
  (:require [cheap-hate.core :as c8]
            [cheap-hate.romloader :as rom]))

(c8/start-machine (rom/load-rom "roms/DEMO_MAZE"))
