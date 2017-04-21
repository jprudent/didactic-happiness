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

(defn hex-distance [a b]
  (cube-distance (grid->cube a) (grid->cube b)))

(defn lerp [a b t]
  {:pre [(<= 0 t 1.0)]}
  (+ a (* (- b a) t)))

(defn cube-lerp [{x1 :x y1 :y z1 :z} {x2 :x y2 :y z2 :z} t]
  {:x (lerp x1 x2 t)
   :y (lerp y1 y2 t)
   :z (lerp z1 z2 t)})

(defn cube-round [floating-cube]
  (let [{rx :x ry :y rz :z :as whole-cube} (into {} (map (fn [[k v]] [k (Math/round (double v))]) floating-cube))
        {x-diff :x y-diff :y z-diff :z} (into {} (map (fn [[k v]] [k (Math/abs (- (get floating-cube k) v))]) whole-cube))]
    (cond
      (and (> x-diff y-diff) (> x-diff y-diff))
      (assoc whole-cube :x (- (- ry) rz))
      (> y-diff z-diff)
      (assoc whole-cube :y (- (- rx) rz))
      :else
      (assoc whole-cube :z (- (- rx) ry)))))

(defn cube-linedraw [a b]
  (let [distance   (cube-distance a b)
        nb-samples (/ 1.0 distance)]
    (map
      #(cube-round (cube-lerp a b (* nb-samples %)))
      (range 0 (inc distance)))))

(defn predict-next-position [{:keys [x y rotation speed] :as ship}]
  (cond
    (zero? speed)
    ship

    (= 1 speed)
    (if (odd? y)
      (condp = rotation
        0
        (update-in ship [:x] inc)
        1
        (-> (update-in ship [:x] inc)
            (update-in [:y] dec))
        2
        (update-in ship [:y] dec)
        3
        (update-in ship [:x] dec)
        4
        (update-in ship [:y] inc)
        5
        (-> (update-in ship [:x] inc)
            (update-in [:y] inc)))
      (condp = rotation
        0
        (update-in ship [:x] inc)
        1
        (update-in ship [:y] dec)
        2
        (-> (update-in ship [:x] dec)
            (update-in [:y] dec))
        3
        (update-in ship [:x] dec)
        4
        (-> (update-in ship [:x] dec)
            (update-in [:y] inc))
        5
        (update-in ship [:y] inc)))

    (= 2 speed)
    (-> (predict-next-position (assoc ship :speed 1))
        (predict-next-position)
        (assoc :speed speed))))

(defn predict-next-positions [target]
  (lazy-seq (let [p (predict-next-position target)]
              (cons p (predict-next-positions p)))))

(defn when-bullet-will-hit? [my-ship target]
  (Math/round (inc (/ (hex-distance my-ship target) 3.0))))

(defn where-should-i-fire? [my-ship target]
  (->> (map (fn [target round]
              [target (Math/abs (- round (when-bullet-will-hit? my-ship target)))])
            (predict-next-positions target)
            (range 5))
       (sort-by second)
       (map first)
       (first)))

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
            #__          #_(debug entity)]

        (recur (dec i) (conj entities entity)))
      entities)))

(defn distance [{coors1 :cube-coor} {coors2 :cube-coor}]
  (cube-distance coors1 coors2))

(defn find-barrels [entities]
  (filter #(= :BARREL (:type %)) entities))

(defn ship? [entity]
  (= :SHIP (:type entity)))

(defn find-enemies [entities]
  (filter #(and (not (:mine? %)) (ship? %)) entities))

(defn find-my-ships
  ([entities] (find-my-ships entities (constantly true)))
  ([entities pred]
   (filter #(and (:mine? %) (ship? %) (pred %)) entities)))

(defn find-ship-by-id [entities id]
  (first (filter #(and (= id (:id %)) (ship? %)) entities)))

(defn my-ship-exist? [my-ship entities]
  (not (nil? (find-ship-by-id entities (:id my-ship)))))

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
        #__             #_(debug (str "Entities :" entities))
        #__             #_(debug (str "An enemy " (prn-str (find-enemy entities))))
        #__             #_(debug (str "A barrel : " (find-barrel entities (constantly true))))]
    [my-ship-count entities]))

(defmulti execute-order (fn [[kind & _]] kind))

(defmethod execute-order :move [[_ target]]
  (move! target))

(defmethod execute-order :wait [_]
  (wait!))

(defmethod execute-order :fire [[_ target]]
  (fire! target))

(defn execute-one-order! [{:keys [doing tick] :as state}]
  (cond

    (:unstuck doing)
    (do (execute-order (get-in state [:doing :unstuck :order]))
        state)

    (:fire-target doing)
    (do (execute-order (get-in state [:doing :fire-target :order]))
        (-> (update-in state [:doing] dissoc :fire-target)
            (update-in [:doing] assoc :cooling-canon {:started-at tick})))

    (:refuel doing)
    (do (execute-order (get-in state [:doing :refuel :order]))
        state)
    :default
    (do (execute-order [:wait])
        state)))

(defn barrel-at-location? [{:keys [order]} entities]
  (let [[_ entity] order
        keys   [:x :y :type]
        target (select-keys entity keys)]
    (find-entity entities #(= (select-keys % keys) target))))

(defn do-not-refuel-on-absent-barrel [{{:keys [refuel]} :doing :as state} entities]
  (if (and refuel (not (barrel-at-location? refuel entities)))
    (update-in state [:doing] dissoc :refuel)
    state))

(defn canon-is-cool [{{:keys [cooling-canon]} :doing tick :tick :as state}]
  (if (and cooling-canon (> tick (inc (:started-at cooling-canon))))
    (update-in state [:doing] dissoc :cooling-canon)
    state))

(defn stuck? [{:keys [my-ship doing history]}]
  (and (:refuel doing)
       (zero? (:speed my-ship))
       (= #{0} (->> (take 2 history)
                    (map :speed)
                    (set)))))

(defn do-not-unstuck-if-moving [state]
  (if (not (stuck? state))
    (update-in state [:doing] dissoc :unstuck)
    state))

(defn clean-doing [state entities]
  (-> (do-not-refuel-on-absent-barrel state entities)
      (canon-is-cool)
      (do-not-unstuck-if-moving)))

(defn ->refuel-order [entities]
  (let [my-ship                (first (find-my-ships entities))
        a-not-too-close-barrel (first (drop 1 (cycle (sort-by (partial distance my-ship) (find-barrels entities)))))]
    [:move (or a-not-too-close-barrel world-center)]))

(defn ->fire-target [my-ship entities]
  [:fire (where-should-i-fire?
           my-ship
           (find-enemy entities))])                                             ;;TODO find-closest-enemy

(def x-max+1 23)
(def y-max+1 21)

(defn ->unstuck-order []
  [:move {:x (rand-int x-max+1) :y (rand-int y-max+1)}])

(defn new-orders [{:keys [doing my-ship tick] :as state} entities]
  (debug my-ship)
  (cond-> state

          (and (stuck? state) (not (:unstuck doing)))
          (assoc-in [:doing :unstuck] {:order      (->unstuck-order)
                                       :started-at tick})
          (not (:refuel doing))
          (assoc-in [:doing :refuel] {:order      (->refuel-order entities)
                                      :started-at tick})
          (not (:cooling-canon doing))
          (assoc-in [:doing :fire-target] {:order      (->fire-target my-ship entities)
                                           :started-at tick})))


(defn handle-one-ship [state entities]
  (debug (str "ship " (:id state) state))
  (let [my-ship (find-ship-by-id entities (get-in state [:my-ship :id]))]
    (-> (assoc-in state [:my-ship] my-ship)
        (update-in [:tick] inc)
        (clean-doing entities)
        (new-orders entities)
        (execute-one-order!)
        (update-in [:history] cons my-ship))))

(defn -main [& _]
  (loop [[_ entities] (read-status)
         states (map (fn [my-ship] {:doing   {}
                                    :my-ship my-ship
                                    :tick    0})
                     (find-my-ships entities))]
    #_(debug (prn-str states))
    (let [states (->> (filter #(my-ship-exist? (:my-ship %) entities) states)
                      (map #(handle-one-ship % entities)))]
      (doall states)
      (debug states)
      (recur
        (read-status)
        states))))

#_(defn -main [& args]
    (binding [*out* *err*]
      (let [my-ship-count (read)
            entity-count  (read)
            _             (println my-ship-count)
            _             (println entity-count)]
        (doseq [i (range entity-count)]
          (println (read) (read) (read) (read) (read) (read) (read) (read))))))

;; idees :
;; - tirer sur l'ennemi le plus proche
;; - tirer sur une mine si elle est proche (2 cases si vitesse = 1, 4 cases si vitesse = 2)
;; - tirer que si la destination est à une distance de 10 au max
;; - si un ennemi est juste derrière, larguer une mine
;; - ne pas faire tirer plusieurs bateaux sur la meme cible
;; - ne pas aller chercher le meme baril de rhum
;; - récupérer le rhum d'un navire coulé
;; - quand l'ennemi a une vitesse de 0, le calcul de prediction est complètement faux
