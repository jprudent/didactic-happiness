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

(defn find-barrel
  ([entities] (find-barrel entities (constantly true)))
  ([entities pred]
   (->> (find-barrels entities)
        (some #(when (pred %) %)))))

(defn find-enemy
  ([entities pred]
   (->> (find-enemies entities)
        (some #(when (pred %) %))))
  ([entities]
   (find-enemy entities (constantly true))))

(defn find-entity
  [entities pred]
  (some #(when (pred %) %) entities))

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

(defn read-status []
  (let [my-ship-count (read)
        entity-count  (read)
        entities      (read-entities! entity-count)
        _             (debug (str "Entities :" entities))
        _             (debug (str "An enemy " (prn-str (find-enemy entities))))
        _             (debug (str "A barrel : " (find-barrel entities (constantly true))))]
    [my-ship-count entities]))

(defmulti execute-order (fn [[kind & _]] kind))

(defmethod execute-order :move-all-my-ships [[_ target]]
  (move! target))


(defmethod execute-order :wait [_]
  (wait!))

(defn execute-one-order! [{[[kind & _ :as order] & others] :orders tick :tick :as state}]
  (debug (str "order " order))
  (if order
    (do (execute-order order)
        (-> (assoc state :orders others)
            (update-in [:doing] assoc kind {:order order :started-at tick})))
    (do
      (execute-order (or (get-in state [:doing :move-all-my-ships :order]) [:wait]))
      state)))

(defn target-at-location? [{:keys [order]} entities]
  (let [[_ entity] order
        keys   [:x :y :type]
        target (select-keys entity keys)]
    (find-entity entities #(= (select-keys % keys) target))))

(defn remove-absent-target [{{:keys [move-all-my-ships] :as doing} :doing :as state} entities]
  (if (not (target-at-location? move-all-my-ships entities))
    (update-in state [:doing] dissoc :move-all-my-ships)
    state))

(defn clean-doing [state entities]
  (-> (remove-absent-target state entities)))

(defn new-orders [{:keys [doing] :as state} entities]
  (if (not (:move-all-my-ships doing))
    (update-in state [:orders] conj [:move-all-my-ships (find-barrel entities)])
    state))

(defn -main [& args]
  (loop [[_ entities :as status] (read-status)
         state {:orders [[:move-all-my-ships (or (find-barrel entities) world-center)]]
                :doing  {}
                :tick   0}]
    (debug (prn-str state))
    (let [state (-> (execute-one-order! state)
                    (clean-doing entities)
                    (new-orders entities)
                    (update-in [:tick] inc))]
      (recur
        (read-status)
        state))))
