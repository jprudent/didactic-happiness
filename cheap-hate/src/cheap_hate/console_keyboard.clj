(ns cheap-hate.console-keyboard
  "An implementation of keyboard for the console.
  It requires the tty to not buffer input (stty -icanon)"
  (:require [cheap-hate.core :as c8]))


(def ^:private r (.reader (System/console)))

(def us-layout {(int \1) 0x1, (int \2) 0x2, (int \3) 0x3, (int \4) 0xC
                (int \q) 0x4, (int \w) 0x5, (int \e) 0x6, (int \r) 0xD
                (int \a) 0x7, (int \s) 0x8, (int \d) 0x9, (int \f) 0xE
                (int \z) 0xA, (int \x) 0x0, (int \c) 0xB, (int \v) 0xF})

(def fr-layout {(int \&) 0x1, (int \é) 0x2, (int \") 0x3, (int \') 0xC
                (int \a) 0x4, (int \z) 0x5, (int \e) 0x6, (int \r) 0xD
                (int \q) 0x7, (int \s) 0x8, (int \d) 0x9, (int \f) 0xE
                (int \w) 0xA, (int \x) 0x0, (int \c) 0xB, (int \v) 0xF})

(def no-key {:key nil :validity 0})

(defn- now [] (System/currentTimeMillis))

(defn- old-buffer? [keyboard]
  (> (- (now) (get (:buffered-key keyboard) :validity 0))
     100))

(defrecord ConsoleKeyboard [layout buffered-key]
  c8/Keyboard
  (read-device [this]
    (if-let [key (if (.ready r)
                   (get-in this [:layout (.read r)])
                   nil)]
      (update this :buffered-key assoc
              :key key
              :validity (now))
      (if (old-buffer? this)
        (assoc this :buffered-key no-key)
        this)))
  (pressed-key [this] (get-in this [:buffered-key :key])))