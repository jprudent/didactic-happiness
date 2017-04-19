(ns Player
  (:gen-class))

(defn debug [msg]
  (binding [*out* *err*]
    (println (prn-str msg))))

(defn read-entities! [entity-count]
  (loop [i        entity-count
         entities []]
    (if (> i 0)
      (let [entityId   (read)
            entityType (keyword (read))
            x          (read)
            y          (read)
            arg1       (read)
            arg2       (read)
            arg3       (read)
            arg4       (read)
            entity     {:id   entityId
                        :type entityType
                        :x    x
                        :y    y
                        }
            entity     (if (= (:type entity) :SHIP)
                         (assoc entity :rotation arg1
                                       :speed arg2
                                       :stock arg3
                                       :mine? (= 1 arg4))
                         (assoc entity :amount arg1))
            _          (debug entity)]

        (recur (dec i) (conj entities entity)))
      entities)))

(defn find-barrels [entities]
  (filter #(= :BARREL (:type %)) entities))

(defn find-enemies [entities]
  (filter #(and (not (:mine? %)) (= :SHIP (:type %))) entities))

(defn find-barrel [entities pred]
  (->> (find-barrels entities)
       (some #(when (pred %) %))))

(defn find-enemy
  ([entities pred]
   (->> (find-enemies entities)
        (some #(when (pred %) %))))
  ([entities]
   (find-enemy entities (constantly true))))

(defn move! [{:keys [x y]}]
  (debug (str "moving to " x y))
  (println "MOVE" x y))

(defn fire! [{:keys [x y]}]
  (debug (str "firing to " x y))
  (println "FIRE" x y))

(defn mine! []
  (println "MINE"))

(defn wait! []
  (println "WAIT"))

(def world-center {:x 11 :y 10})

(defn move-ship-to-any-barrel! [entities]
  (move! (or (find-barrel entities (constantly true)) world-center)))

(defn find-a-target-and-fire! [entities]
  (fire! (or (find-enemy entities) world-center)))

(defmulti do-action (fn [type _ _] type))

(defmethod do-action :move-all-my-ships [_ my-ship-count entities]
  (doseq [_ (range my-ship-count)]
    (move-ship-to-any-barrel! entities)))

(defmethod do-action :fire-all-my-ships [_ my-ship-count entities]
  (doseq [_ (range my-ship-count)]
    (find-a-target-and-fire! entities)))

(defmethod do-action :mine-all-my-ships [_ my-ship-count _]
  (doseq [_ (range my-ship-count)]
    (mine!)))

(defmethod do-action :wait-all-my-ships [_ my-ship-count _]
  (doseq [_ (range my-ship-count)]
    (wait!)))

(defn -main [& args]
  (doseq [action (cycle [:move-all-my-ships
                         :fire-all-my-ships
                         :move-all-my-ships
                         :fire-all-my-ships
                         :mine-all-my-ships])]
    (let [my-ship-count (read)
          entity-count  (read)
          entities      (read-entities! entity-count)]
      (do-action action my-ship-count entities))))

; Auto-generated code below aims at helping you parse
; the standard input according to the problem statement.

#_(defn -main [& args]
    (while true
      (let [myShipCount (read)
            entityCount (read)]
        ; myShipCount: the number of remaining ships
        ; entityCount: the number of entities (e.g. ships, mines or cannonballs)

        (loop [i        entityCount
               entities []]
          (when (> i 0)
            (let [entityId   (read)
                  entityType (read)
                  x          (read)
                  y          (read)
                  arg1       (read)
                  arg2       (read)
                  arg3       (read)
                  arg4       (read)]
              (recur (dec i)))))

        (loop [i myShipCount]
          (when (> i 0)

            ; (binding [*out* *err*]
            ;   (println "Debug messages..."))

            ; Any valid action, such as "WAIT" or "MOVE x y"
            (print "MOVE")
            (print " ")
            (print "11")
            (print " ")
            (print "10")
            (println "")
            (recur (dec i)))))))