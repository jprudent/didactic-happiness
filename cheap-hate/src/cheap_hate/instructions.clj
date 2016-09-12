(ns cheap-hate.instructions
  "This namespace contains all the logic of execution any instructions,
  independently of the underlying machine implementation."
  (:require [cheap-hate.core :refer :all]
            [cheap-hate.bits-util :as bits]))

(defn- set-registers [[machine & registers]]
  (assoc-registers machine registers))

(defn- get-registers [machine registers]
  (apply vector machine
         (map (juxt identity (partial get-register machine)) registers)))


;; Misc instructions

(defmulti execute (fn [_ instruction] (first instruction)))
(defmethod execute :sys [machine _]
  (inc-pc machine))

(defmethod execute :halt [_ _] nil)


;; Instructions that change the I register

(defmethod execute :mov-i [machine [_ address]]
  (-> (assoc-i machine address)
      inc-pc))

(letfn [(add-16bits [nn] (comp bits/two-lowest-bytes (partial + nn)))
        (call-update-i [[machine [_ vx]]] (update-i machine (add-16bits vx)))]
  (defmethod execute :add-i [machine [_ x]]
    (-> (get-registers machine [x])
        call-update-i
        inc-pc)))

(letfn [(bdc [n] (map #(Integer/valueOf (str %)) (format "%03d" n)))
        (assoc-mem-bdc [[machine [_ vx]]]
          (assoc-mem machine (get-i machine) (bdc vx)))]
  (defmethod execute :mov-i-decimal [machine [_ x]]
    (-> (get-registers machine [x])
        assoc-mem-bdc
        inc-pc)))


;; Jump instructions

(defmethod execute :mov-i-font [machine [_ x]]
  (-> (assoc-i machine (* (get-register machine x) 5))
      inc-pc))

(defmethod execute :call [machine [_ address]]
  (-> (push-stack machine)
      (assoc-pc address)))

(defmethod execute :return [machine _]
  (-> (assoc-pc machine (peek-stack machine))
      pop-stack))

(defmethod execute :jump [machine [_ address]]
  (assoc-pc machine address))

(letfn [(skip-if [machine op x y]
          (-> (if (op x y) (inc-pc machine) machine)
              inc-pc))]

  (defmethod execute :skip-if-value [machine [_ x test-op nn]]
    (skip-if machine (resolve test-op) (get-register machine x) nn))

  (defmethod execute :skip-if-register [machine [_ x test-op y]]
    (skip-if machine (resolve test-op) (get-register machine x) (get-register machine y)))

  (defmethod execute :skip-if-key [machine [_ test-op x]]
    (skip-if machine (resolve test-op) (get-register machine x) (get-keyboard machine))))

(defmethod execute :jmp-add-pc-v0 [machine [_ addr]]
  (assoc-pc machine (+ (get-register machine 0) addr)))


;; Instructions that change registers

(defmethod execute :mov-register-value [machine [_ x nn]]
  (-> (set-registers [machine x nn])
      inc-pc))

(letfn [(switch-vals [[machine [x _] [_ vy]]] [machine x vy])]
  (defmethod execute :mov-register-register [machine [_ x y]]
    (-> (get-registers machine [x y])
        switch-vals
        set-registers
        inc-pc)))

(letfn [(add-8bits [nn] (comp bits/lowest-byte (partial + nn)))]
  (defmethod execute :add-register-value [machine [_ x nn]]
    (-> (update-register machine x (add-8bits nn))
        inc-pc)))

(defmethod execute :mov-register-delay-timer [machine [_ x]]
  (-> (set-registers [machine x (get-delay-timer machine)])
      inc-pc))

(defmethod execute :mov-register-random [machine [_ x nn]]
  (letfn [(random [machine] [machine x (bit-and nn (get-prn machine))])]
    (-> (update-prng machine)
        random
        set-registers
        inc-pc)))

(defmethod execute :mov-register-key [machine [_ x]]                            ;; This implementation will loop like hell til no key is pressed
  (if-let [key (get-keyboard machine)]
    (inc-pc (set-registers [machine x key]))
    machine))

(defmethod execute :mov-registers-memory [machine [_ n]]
  (-> (assoc-registers
        machine
        (interleave (range) (read-memory machine (get-i machine) (inc n))))
      inc-pc))

(letfn [(arithmetic [apply-op machine x y]
          (-> (get-registers machine [x y])
              apply-op
              set-registers
              inc-pc))]

  (letfn [(arithmetic-with-carry [machine x y op carry?]
            (letfn [(apply-op [[machine [x vx] [_ vy]]]
                      (let [r (op vx vy)]
                        [machine, x (bits/lowest-byte r), 0xF (if (carry? r) 1 0)]))]
              (arithmetic apply-op machine x y)))]

    (defmethod execute :add-register-register [machine [_ x y]]
      (arithmetic-with-carry machine x y + #(> % 0xFF)))

    (defmethod execute :sub-register-register [machine [_ x y]]
      (arithmetic-with-carry machine x y - pos?))

    (defmethod execute :sub-reverse-register-register [machine [_ x y]]
      (arithmetic-with-carry machine x y #(- %2 %1) pos?)))

  (letfn [(arithmetic-without-carry [machine x y boolean-op]
            (letfn [(apply-op [[machine [x vx] [_ vy]]]
                      [machine, x (boolean-op vx vy)])]
              (arithmetic apply-op machine x y)))]

    (defmethod execute :or-register-register [machine [_ x y]]
      (arithmetic-without-carry machine x y bit-or))

    (defmethod execute :and-register-register [machine [_ x y]]
      (arithmetic-without-carry machine x y bit-and))

    (defmethod execute :xor-register-register [machine [_ x y]]
      (arithmetic-without-carry machine x y bit-xor))))

(letfn [(shift [machine x shift-direction carry]
          (letfn [(shift [[machine [x vx]]]
                    [machine, x (shift-direction vx 1), 0xF (carry vx)])]
            (-> (get-registers machine [x])
                shift
                set-registers
                inc-pc)))]

  (defmethod execute :shift-right-register [machine [_ x]]
    (shift machine x bit-shift-right bits/lowest-bit))

  (defmethod execute :shift-left-register [machine [_ x]]
    (shift machine x (comp bits/lowest-byte bit-shift-left) bits/highest-bit)))


;; Instructions that change timers

(letfn [(set-timer [machine x set-x-timer]
          (letfn [(call-set-timer [[machine [_ vx]]] (set-x-timer machine vx))]
            (-> (get-registers machine [x])
                call-set-timer
                inc-pc)))]

  (defmethod execute :mov-sound-timer [machine [_ x]]
    (set-timer machine x assoc-sound-timer))

  (defmethod execute :mov-delay-timer [machine [_ x]]
    (set-timer machine x assoc-delay-timer)))


;; Instructions that update the screen memory

(defmethod execute :clear-screen [machine _]
  (-> (reset-screen-memory machine)
      inc-pc))



(letfn [(one-byte-sprite-bits [one-byte-sprite]
          (map (fn [bit-num] (bits/bit-at bit-num one-byte-sprite)) (range 8)))

        (flip-pixel [machine x y pixel]
          (if (pos? pixel)
            (let [real-x       (mod x 64)
                  real-y       (mod y 32)
                  actual-pixel (get-pixel machine real-x real-y)]
              (if (= actual-pixel pixel)
                (-> (assoc-registers machine [0xF 1])
                    (unset-pixel real-x real-y))
                (set-pixel machine real-x real-y)))
            machine))

        (print-1-byte-sprite [machine [x y one-byte-sprite]]
          (reduce (fn [machine [bit-num pixel]] (flip-pixel machine (+ x bit-num) y pixel))
                  machine
                  (map vector (range) (one-byte-sprite-bits one-byte-sprite))))

        (print-sprite
          [machine x y sprite]
          (reduce print-1-byte-sprite
                  (set-registers [machine 0xF 0])
                  (map #(vector %1 (+ y %2) %3)
                       (repeat x) (range) sprite)))]

  (defmethod execute :draw [machine [_ x y size]]
    (letfn [(draw [[machine [_ vx] [_ vy]]]
              (let [sprite (read-memory machine (get-i machine) size)]
                (print-sprite machine vx vy sprite)))]
      (-> (get-registers machine [x y])
          draw
          inc-pc))))


;; Instructions that change memory

(letfn [(call-set-mem [[machine & registers]]
          (assoc-mem machine (get-i machine) (map second registers)))]

  (defmethod execute :set-memory [machine [_ x]]
    (-> (get-registers machine (range 0 (inc x)))
        call-set-mem
        inc-pc)))