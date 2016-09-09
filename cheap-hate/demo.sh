#!/bin/sh
setterm --cursor off
stty raw;~/bin/java/bin/java -cp ~/.m2/repository/org/clojure/clojure/1.8.0/clojure-1.8.0.jar:src clojure.main src/cheap_hate/console_keyboard.clj
