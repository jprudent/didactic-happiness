(ns cheap-hate.looping-clock
  "An implementation of a quartz that will crazily loop until desired
  period is reached."
  (:require [cheap-hate.core :as core]))

(defn- now [] (System/currentTimeMillis))

(defrecord LoopingClock [frequency last-timestamp]
  core/Clock
  (throttle [this]
    (loop [{:keys [last-timestamp frequency] :as quartz} this]
      (let [t      (now)
            period (/ 1000 frequency)
            delta  (- t last-timestamp)]
        (if (> delta period)
          (assoc quartz :last-timestamp t)
          (recur quartz))))))

