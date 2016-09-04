(ns cheap-hate.core
  (:import (java.util Random)))

;; This is the array of bitmap fonts
;; Each line represents a 8x5 pixels character
(def ^:static fonts
  [0xF0 0x90 0x90 0x90 0xF0                                 ;; 0
   0x20 0x60 0x20 0x20 0x70                                 ;; 1
   0xF0 0x10 0xF0 0x80 0xF0                                 ;; 2
   0xF0 0x10 0xF0 0x10 0xF0                                 ;; 3
   0x90 0x90 0xF0 0x10 0x10                                 ;; 4
   0xF0 0x80 0xF0 0x10 0xF0                                 ;; 5
   0xF0 0x80 0xF0 0x90 0xF0                                 ;; 6
   0xF0 0x10 0x20 0x40 0x40                                 ;; 7
   0xF0 0x90 0xF0 0x90 0xF0                                 ;; 8
   0xF0 0x90 0xF0 0x10 0xF0                                 ;; 9
   0xF0 0x90 0xF0 0x90 0x90                                 ;; A
   0xE0 0x90 0xE0 0x90 0xE0                                 ;; B
   0xF0 0x80 0x80 0x80 0xF0                                 ;; C
   0xE0 0x90 0x90 0x90 0xE0                                 ;; D
   0xF0 0x80 0xF0 0x80 0xF0                                 ;; E
   0xF0 0x80 0xF0 0x80 0x80                                 ;; F
   ])

(def ^:static interpreter-code (concat fonts (repeat (- 0x200 (count fonts)) 0)))

;; A fresh machine craving for a program to run
(def ^:static fresh-machine {:RAM           (concat interpreter-code
                                                    (repeat (- 0x1000 0x200) 0))
                             :registers     (vec (repeat 0xF 0))
                             :I             0
                             :PC            0
                             :stack         []
                             :screen-memory 0
                             :delay-timer   0
                             :sound-timer   0
                             :prn           42})

(defn power-of-2 [exp] (bit-shift-left 1 exp))
(defn mask-of-size [size] (dec (power-of-2 size)))
(defn nth-word
  "returns the nth word in x, 0 being the righmost position.
  The word size is specified in bits

  (map (fn [nth] (nth-word 12 nth 0xABCD)) [0 1 2 3])
  => (0xBCD 0xA 0 0)"
  [word-size nth x]
  (let [bits   (* nth word-size)
        mask   (bit-shift-left (mask-of-size word-size) bits)
        masked (bit-and x mask)]
    (bit-shift-right masked bits)))

(def w0 (partial nth-word 4 0))
(def w3 (partial nth-word 4 3))
(def w1 (partial nth-word 4 1))
(defn w3-w1-w0 [opcode] [(w3 opcode) (w1 opcode) (w0 opcode)])
(def address (partial nth-word 12 0))
(def vx (partial nth-word 4 2))
(def vy (partial nth-word 4 1))
(def nn (partial nth-word 8 0))
(def height (partial nth-word 4 0))
(def lowest-byte (partial nth-word 8 0))
(def lowest-bit (partial nth-word 1 0))
(def highest-bit (partial nth-word 1 7))

(defn debug [msg arg]
  (println (if (integer? arg) (str msg (Integer/toHexString arg)) (str msg arg)))
  arg)

(defn opcode->instruction
  "extract informations from opcode"
  [opcode]
  (let [[w3 _ w0 :as w3-w1-w0] (w3-w1-w0 opcode)
        w3-w0 [w3 w0]]
    (mapv #(if (fn? %) (% opcode) %)
          (cond
            (= opcode 0) [:halt]                            ;; this one, I made up for testing purpose
            (= opcode 0x00E0) [:clear-screen]
            (= opcode 0x00EE) [:return]
            (= 0 w3) [:sys address]
            (= 1 w3) [:jump address]
            (= 2 w3) [:call address]
            (= 3 w3) [:skip-if-value vx '= nn]
            (= 4 w3) [:skip-if-value vx 'not= nn]
            (= [5 0] [w3 w0]) [:skip-if-register vx '= vy]
            (= 6 w3) [:mov-value vx nn]
            (= 7 w3) [:add-value vx nn]
            (= [8 0] w3-w0) [:mov-register vx vy]
            (= [8 1] w3-w0) [:or vx vy]
            (= [8 2] w3-w0) [:and vx vy]
            (= [8 3] w3-w0) [:xor vx vy]
            (= [8 4] w3-w0) [:add-register vx vy]
            (= [8 5] w3-w0) [:sub-register vx vy]
            (= [8 6] w3-w0) [:shift-right vx]
            (= [8 7] w3-w0) [:sub-reverse-register vx vy]
            (= [8 0xE] w3-w0) [:shift-left vx]
            (= [9 0] w3-w0) [:skip-if-register vx 'not= vy]
            (= 0xA w3) [:mov-i address]
            (= 0xB w3) [:jmp-add-v0 address]
            (= 0xC w3) [:random vx nn]
            (= [0xF 1 8] w3-w1-w0) [:set-sound-timer vx]
            (= [0xF 1 0xE] w3-w1-w0) [:add-i vx]
            (= [0xF 2 9] w3-w1-w0) [:set-font-ip vx]
            (= [0xF 3 3] w3-w1-w0) [:set-ip-decimal vx]
            (= 0xD w3) [:draw vx vy height]
            (= [0xE 9 0xE] w3-w1-w0) [:skip-if-key '= vx]
            (= [0xE 0xA 1] w3-w1-w0) [:skip-if-key 'not= vx]
            (= [0xF 0 7] w3-w1-w0) [:mov-timer vx]
            (= [0xF 0 0xA] w3-w1-w0) [:mov-wait-key vx]
            (= [0xF 1 5] w3-w1-w0) [:set-delay-timer vx]
            (= [0xF 5 5] w3-w1-w0) [:set-memory vx]
            (= [0xF 6 5] w3-w1-w0) [:set-registers vx]))))

(defn inc-pc [machine] (update machine :PC + 2))
(defn set-pc [f] (fn [machine] (assoc machine :PC (f machine))))
(defn reset-screen-memory [machine] (assoc machine :screen-memory 0))
(defn push-stack [machine] (update machine :stack conj (+ 2 (:PC machine))))
(defn pop-stack [machine] (update machine :stack pop))
(defn set-i [const] (fn [machine] (assoc machine :I const)))
(defn update-register [x update-vx] (fn [machine] (update machine :registers update x update-vx)))
(defn set-registers [[machine x v & others]] (apply update machine :registers assoc x v others))
(defn set-delay-timer [v machine] (assoc machine :delay-timer v))
(defn set-sound-timer [v machine] (assoc machine :sound-timer v))
(defn get-register [x machine] (get-in machine [:registers x]))
(defn get-registers [& registers] (apply juxt identity
                                         (map #(fn [machine] [%1 (get-register %1 machine)]) registers)))
(defn next-int [old-seed] (lowest-byte (+ 3 old-seed)))      ;; TODO have a proper prng
(defn update-prng [machine] (update machine :prn next-int))
(defn get-prng [machine] (get machine :prn))
(defn get-i [machine] (get machine :I))

(defmulti command first)
(defmethod command :halt [_] (constantly nil))
(defmethod command :clear-screen [_] (comp inc-pc reset-screen-memory))
(defmethod command :return [_] (comp pop-stack (set-pc (fn [m] (peek (:stack m))))))
(defmethod command :sys [_] inc-pc)
(defmethod command :jump [[_ address]] (set-pc (constantly address)))
(defmethod command :call [[_ address]] (comp (set-pc (constantly address)) push-stack))
(defmethod command :mov-i [[_ address]] (comp inc-pc (set-i address)))
(defn skip-if [op fx fy]
  (fn [machine]
    (-> (if (op (fx machine) (fy machine)) (inc-pc machine) machine)
        inc-pc)))
(defmethod command :skip-if-value [[_ vx test-op const]]
  (skip-if (resolve test-op) (partial get-register vx) (constantly const)))
(defmethod command :skip-if-register [[_ vx test-op vy]]
  (skip-if (resolve test-op) (partial get-register vx) (partial get-register vy)))
(defmethod command :mov-value [[_ vx nn]] (comp inc-pc (update-register vx (constantly nn))))
(defmethod command :add-value [[_ vx nn]] (comp inc-pc (update-register vx (comp lowest-byte (partial + nn)))))
(defn arithmetic [x y op carry?]
  (comp inc-pc
        set-registers
        (fn [[machine [x vx] [_ vy]]]
          (let [r (op vx vy)]
            [machine, x (lowest-byte r), 0xF (if (carry? r) 1 0)]))
        (get-registers x y)))
(defmethod command :add-register [[_ x y]] (arithmetic x y + #(> % 0xFF)))
(defmethod command :sub-register [[_ x y]] (arithmetic x y - pos?))
(defmethod command :sub-reverse-register [[_ x y]] (arithmetic x y #(- %2 %1) pos?))
(defmethod command :mov-register [[_ vx vy]]
  (comp inc-pc
        set-registers
        (fn [[machine [x _] [_ vy]]] [machine x vy])
        (get-registers vx vy)))
(defmethod command :jmp-add-v0 [[_ addr]] (set-pc (fn [machine] (+ (get-register 0 machine) addr))))
(defn- boolean-command [x y boolean-op]
  (comp
    inc-pc
    set-registers
    (fn [[machine [x vx] [_ vy]]] [machine, x (boolean-op vx vy)])
    (get-registers x y)))
(defmethod command :or [[_ x y]] (boolean-command x y bit-or))
(defmethod command :and [[_ x y]] (boolean-command x y bit-and))
(defmethod command :xor [[_ x y]] (boolean-command x y bit-xor))
(defn shift [x direction carry]
  (comp
    inc-pc
    set-registers
    (fn [[machine [x vx]]] [machine, x (direction vx 1), 0xF (carry vx)])
    (get-registers x)))
(defmethod command :shift-right [[_ x]] (shift x bit-shift-right lowest-bit))
(defmethod command :shift-left [[_ x]] (shift x (comp lowest-byte bit-shift-left) highest-bit))
(defmethod command :random ([[_ x nn]]
                             (comp
                               inc-pc
                               set-registers
                               (fn [machine] [machine x (bit-and nn (get-prng machine))])
                               update-prng)))
(defmethod command :set-sound-timer [[_ x]]
  (comp
    inc-pc
    (fn [[machine [_ x]]] (set-sound-timer x machine))
    (get-registers x)))
(defmethod command :set-delay-timer [[_ x]]
  (comp
      inc-pc
      (fn [[machine [_ x]]] (set-delay-timer x machine))
      (get-registers x)))
(defmethod command :add-i [[_ x]]
  (comp inc-pc
        (fn [[machine [_ vx]]] ((set-i (lowest-byte (+ (get-i machine) vx))) machine))
        (get-registers x)))


(defn load-program [machine program]
  (-> (assoc machine :RAM (concat interpreter-code program)) ;; TODO 0 padding ?
      (assoc :PC 0x200)))

(defn concat-bytes [b1 b2] (bit-or (bit-shift-left b1 8) b2))
(defn byte-at-pc [machine pc-fn] (nth (:RAM machine) (pc-fn (:PC machine))))
(defn read-opcode
  "returns a 2 bytes number at program counter"
  [machine]
  (concat-bytes
    (byte-at-pc machine identity) (byte-at-pc machine inc)))

(defn print-screen! [machine] (println "print screen"))

(defn start-machine [program]
  (let [machine (load-program fresh-machine program)]
    (loop [machine machine]
      (print-screen! machine)
      (debug "@" (:PC machine))
      (let [opcode              (debug "opcode:" (read-opcode machine))
            instruction         (debug "instruction:" (opcode->instruction opcode))
            execute-instruction (command instruction)
            new-machine         (execute-instruction machine)]
        (if new-machine (recur new-machine) machine)))))
