(ns cheap-hate.console-keyboard
  (:require [cheap-hate.core :as c8]))

(def r (-> (System/console) .reader)
  (defrecord ConsoleKeyboard []
    c8/Keyboard
    (pressed-key [_]
      (.read r))))

(let [kb (->ConsoleKeyboard)]
  (while true
    (println (c8/pressed-key kb))))