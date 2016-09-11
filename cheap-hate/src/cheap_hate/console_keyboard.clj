(ns cheap-hate.console-keyboard
  "An implementation of keyboard for the console.
  The console should have a qwerty keymap (setxkbmap us)
  and tty should not buffer input (stty -icanon)"
  (:require [cheap-hate.core :as c8]))



(def r (-> (System/console) .reader))

(def us-layout {(int \1) 0x1, (int \2) 0x2, (int \3) 0x3, (int \4) 0xC
                (int \q) 0x4, (int \w) 0x5, (int \e) 0x6, (int \r) 0xD
                (int \a) 0x7, (int \s) 0x8, (int \d) 0x9, (int \f) 0xE
                (int \z) 0xA, (int \x) 0x0, (int \c) 0xB, (int \v) 0xF})

(def fr-layout {(int \&) 0x1, (int \é) 0x2, (int \") 0x3, (int \') 0xC
                (int \a) 0x4, (int \z) 0x5, (int \e) 0x6, (int \r) 0xD
                (int \q) 0x7, (int \s) 0x8, (int \d) 0x9, (int \f) 0xE
                (int \w) 0xA, (int \x) 0x0, (int \c) 0xB, (int \v) 0xF})

(def no-key {:key nil :validity 0})
(def buffered-key (atom no-key))
(defn- old-buffer? []
  (> (- (System/currentTimeMillis) (get @buffered-key :validity 0)) 100))
(defrecord ConsoleKeyboard []
  c8/Keyboard
  (pressed-key [_]
    (:key (if-let [key (if (.ready r) (get fr-layout (.read r) nil) nil)]
            (swap! buffered-key assoc :key key :validity (System/currentTimeMillis))
            (if (old-buffer?) (reset! buffered-key no-key) @buffered-key)))))