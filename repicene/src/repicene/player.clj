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

(defn read-status []
  (let [my-ship-count (read)
        entity-count  (read)
        entities      (read-entities! entity-count)
        _             (debug (str "Entities :" entities))
        _             (debug (str "An enemy " (prn-str (find-enemy entities))))
        _             (debug (str "A barrel : " (find-barrel entities (constantly true))))]
    [my-ship-count entities]))

(defmulti execute-order (fn [[kind & _]] kind))

(defmethod execute-order :move [[_ target]]
  (move! target))

(defmethod execute-order :wait [_]
  (wait!))

(defmethod execute-order :fire [[_ target]]
  (fire! target))

(defn execute-one-order! [{[[kind & _ :as order] & others] :orders tick :tick :as state}]
  (debug (str "order " order))
  (if order
    (do (execute-order order)
        (-> (assoc state :orders others)
            (update-in [:doing] assoc kind {:order order :started-at tick})))
    (do
      (execute-order (or (get-in state [:doing :move :order]) [:wait]))
      state)))

(defn target-at-location? [{:keys [order]} entities]
  (let [[_ entity] order
        keys   [:x :y :type]
        target (select-keys entity keys)]
    (find-entity entities #(= (select-keys % keys) target))))

(defn do-not-move-on-absent-barrel [{{:keys [move]} :doing :as state} entities]
  (if (and move (not (target-at-location? move entities)))
    (update-in state [:doing] dissoc :move)
    state))

(defn i-am-not-firing-when-canon-is-cool [{{:keys [fire]} :doing tick :tick :as state}]
  (if (and fire (> tick (inc (:started-at fire))))
    (update-in state [:doing] dissoc :fire)
    state))

(defn clean-doing [state entities]
  (-> (do-not-move-on-absent-barrel state entities)
      (i-am-not-firing-when-canon-is-cool)))

(defn new-order [state order]
  (update-in state [:orders] conj order))

(defn ->move-order [entities]
  [:move (or (find-barrel entities) world-center)])

(defn new-orders [{:keys [doing] :as state} entities]
  (cond

    (not (:move doing))
    (new-order state (->move-order entities))

    (not (:fire doing))
    (new-order state [:fire (or (find-enemy entities) world-center)])

    :default
    state))

(defn -main [& args]
  (loop [[_ entities :as status] (read-status)
         state {:orders [(->move-order entities)]
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
