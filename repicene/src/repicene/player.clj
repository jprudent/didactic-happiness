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

(defn find-barrel [entities pred]
  (->> (find-barrels entities)
       (some #(when (pred %) %))))

(defn move! [x y]
  (debug (str "moving to " x y))
  (println (str "MOVE " x " " y)))

(defn move-ship-to-any-barrel! [entities]
  (when-let [{:keys [x y]} (find-barrel entities (constantly true))]
    (move! x y)))

(defn -main [& args]
  (while true
    (let [my-ship-count (read)
          entity-count  (read)
          entities      (read-entities! entity-count)
          _             (debug (str "Entities :" entities))
          _             (debug (str "Barrels " (prn-str (find-barrels entities))))
          _             (debug (str "A barrel : " (find-barrel entities (constantly true))))]

      (doseq [_ (range my-ship-count)]
        (move-ship-to-any-barrel! entities)))))

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