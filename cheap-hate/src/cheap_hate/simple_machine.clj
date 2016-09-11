(ns cheap-hate.simple-machine
  (:require [cheap-hate.core :as core]
            [cheap-hate.bits-util :refer [two-lowest-bytes]]
            [cheap-hate.bits-util :as bits]
            [cheap-hate.parser :as parser]
            [cheap-hate.instructions :as instructions]))

(def ^:static empty-screen (vec (for [_ (range 32)] (vec (repeat 64 0)))))

;; This is the array of bitmap fonts
;; Each line represents a 8x5 pixels character
(def ^:static fonts
  [0xF0 0x90 0x90 0x90 0xF0                                                     ;; 0
   0x20 0x60 0x20 0x20 0x70                                                     ;; 1
   0xF0 0x10 0xF0 0x80 0xF0                                                     ;; 2
   0xF0 0x10 0xF0 0x10 0xF0                                                     ;; 3
   0x90 0x90 0xF0 0x10 0x10                                                     ;; 4
   0xF0 0x80 0xF0 0x10 0xF0                                                     ;; 5
   0xF0 0x80 0xF0 0x90 0xF0                                                     ;; 6
   0xF0 0x10 0x20 0x40 0x40                                                     ;; 7
   0xF0 0x90 0xF0 0x90 0xF0                                                     ;; 8
   0xF0 0x90 0xF0 0x10 0xF0                                                     ;; 9
   0xF0 0x90 0xF0 0x90 0x90                                                     ;; A
   0xE0 0x90 0xE0 0x90 0xE0                                                     ;; B
   0xF0 0x80 0x80 0x80 0xF0                                                     ;; C
   0xE0 0x90 0x90 0x90 0xE0                                                     ;; D
   0xF0 0x80 0xF0 0x80 0xF0                                                     ;; E
   0xF0 0x80 0xF0 0x80 0x80                                                     ;; F
   ])

(defn zeroes-padding [bytes final-size]
  (concat bytes (repeat (- final-size (count bytes)) 0)))

(def ^:static interpreter-code (zeroes-padding fonts 0x200))

(letfn [(dectimer [v] (max 0 (dec v)))]
  (defn- update-timers [machine]
    (-> (core/update-sound-timer machine dectimer)
        (core/update-delay-timer dectimer))))

(defrecord SimpleMachine
  [RAM registers I PC keyboard stack screen delay-timer sound-timer prn]

  core/UpdatableMachine
  (load-program [machine program]
    (let [used-mem (concat interpreter-code program)
          mem      (vec (zeroes-padding used-mem 0x1000))]
      (-> (assoc machine :RAM mem)
          (assoc :PC 0x200))))
  (inc-pc [this] (update this :PC + 2))
  (assoc-pc [this address] (assoc this :PC address))
  (push-stack [this] (update this :stack conj (+ 2 (:PC this))))
  (pop-stack [this] (update this :stack pop))
  (assoc-i [this nnn] (assoc this :I nnn))
  (update-i [this f] (update this :I f))
  (update-register [this x f] (update this :registers update x f))
  (assoc-registers [this [x v & others]]
    (apply update this :registers assoc x v others))
  (assoc-delay-timer [this v] (assoc this :delay-timer v))
  (assoc-sound-timer [this v] (assoc this :sound-timer v))
  (update-sound-timer [this f] (update this :sound-timer f))
  (update-delay-timer [this f] (update this :delay-timer f))
  (update-prng [this] (update this :prn (comp two-lowest-bytes (partial + 3)))) ;; This is the worst prng you could imagine
  (assoc-mem [this address values]
    (loop [this    this
           address address
           values  values]
      (if (empty? values)
        this
        (recur (update this :RAM assoc address (first values))
               (inc address)
               (rest values)))))
  (reset-screen-memory [this] (assoc this :screen empty-screen))
  (set-pixel [this x y] (assoc-in this [:screen y x] 1))
  (unset-pixel [this x y] (assoc-in this [:screen y x] 0))
  (assoc-keyboard [this key] (assoc this :keyboard key))

  core/InspectableMachine
  (get-register [machine x] (get-in machine [:registers x]))
  (get-prn [machine] (get machine :prn))
  (get-i [machine] (get machine :I))
  (read-memory [machine address size] (subvec (:RAM machine) address (+ address size)))
  (get-pc [machine] (:PC machine))
  (get-pixel [machine x y] (get-in machine [:screen y x]))
  (get-delay-timer [machine] (get machine :delay-timer))
  (peek-stack [machine] (peek (:stack machine)))
  (get-keyboard [machine] (:keyboard machine))

  core/RunnableMachine
  (start-machine [fresh-machine
                  {:keys [screen flight-recorder keyboard program quartz]}]
    (let [machine (core/load-program fresh-machine program)]
      (loop [machine  machine
             screen   screen
             keyboard keyboard
             quartz   quartz]
        (let [[b1 b2] (core/read-memory machine (core/get-pc machine) 2)
              opcode       (bits/concat-bytes b1 b2)
              instruction  (parser/opcode->instruction opcode)
              new-machine  (instructions/execute machine instruction)
              new-screen   (core/print-screen screen new-machine instruction)
              new-keyboard (core/read-device keyboard)]
          (core/record flight-recorder new-machine opcode)
          (if new-machine                                                       ;; new-machine is nil when opcode = 0 (see :halt)
            (recur (-> (update-timers new-machine)
                       (core/assoc-keyboard (core/pressed-key new-keyboard)))
                   new-screen
                   new-keyboard
                   (core/throttle quartz))
            machine))))))


;; A fresh machine craving for a program to run
(def ^:static fresh-machine
  (map->SimpleMachine {:RAM         (vec (concat interpreter-code
                                                 (repeat (- 0x1000 0x200) 0)))
                       :registers   (vec (repeat 16 0))
                       :I           0
                       :PC          0
                       :stack       []
                       :screen      empty-screen
                       :delay-timer 0
                       :sound-timer 0
                       :prn         42
                       :keyboard    nil}))
