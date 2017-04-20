(ns Player
  (:gen-class))

;; GEOMETRY

;;# convert cube to odd-r offset
;;col = x + (z - (z&1)) / 2
;;row = z

(defn cube->grid [{:keys [x z]}]
  {:x (+ x (/ (- z (bit-and z 1)) 2))
   :y z})

;;# convert odd-r offset to cube
;;x = col - (row - (row&1)) / 2
;;z = row
;;y = -x-z
(defn grid->cube [{:keys [x y]}]
  (let [cx (- x (/ (- y (bit-and y 1)) 2))]
    {:x cx
     :y (- (* -1 cx) y)
     :z y}))

(defn cube-distance [{x1 :x y1 :y z1 :z} {x2 :x y2 :y z2 :z}]
  (/ (+ (Math/abs (- x1 x2)) (Math/abs (- y1 y2)) (Math/abs (- z1 z2))) 2))

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
            entity     (condp = (:type entity)

                         :SHIP
                         (assoc entity :rotation arg1
                                       :speed arg2
                                       :stock arg3
                                       :mine? (= 1 arg4))
                         :BARREL
                         (assoc entity :amount arg1)

                         :CANNONBALL
                         (assoc entity :entity-id arg1 :ticks-before-hit arg2)

                         :MINE
                         entity)
            entity     (assoc entity :cube-coor (grid->cube entity))
            _          (debug entity)]

        (recur (dec i) (conj entities entity)))
      entities)))

(defn distance [{coors1 :cube-coor} {coors2 :cube-coor}]
  (cube-distance coors1 coors2))

(defn find-barrels [entities]
  (filter #(= :BARREL (:type %)) entities))

(defn find-enemies [entities]
  (filter #(and (not (:mine? %)) (= :SHIP (:type %))) entities))

(defn find-my-ships [entities]
  (filter #(and (:mine? %) (= :SHIP (:type %))) entities))

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
  (let [my-ship (first (find-my-ships entities))
        a-not-too-close-barrel  (first (drop 1 (cycle (sort-by (partial distance my-ship) (find-barrels entities)))))]
    [:move (or a-not-too-close-barrel world-center)]))

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
