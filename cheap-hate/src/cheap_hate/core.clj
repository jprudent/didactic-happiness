(ns cheap-hate.core)

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
                             :sound-timer   0})

(defn power-of-2 [exp]
  (bit-shift-left 1 exp))

(defn mask-of-size [size]
  (dec (power-of-2 size)))

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

(defn debug [msg arg]
  (println (str msg arg))
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
            (= [5 0] [w3 w0]) [:skip-if vx '= vy]
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
            (= [9 0] w3-w0) [:skip-if vx 'not= vy]
            (= 0xA w3) [:mov-i address]
            (= 0xB w3) [:jmp-add-v0 address]
            (= 0xC w3) [:random vx nn]
            (= 0xD w3) [:draw vx vy height]
            (= [0xE 9 0xE] w3-w1-w0) [:skip-if-key '= vx]
            (= [0xE 0xA 1] w3-w1-w0) [:skip-if-key 'not= vx]
            (= [0xF 0 7] w3-w1-w0) [:mov-timer vx]
            (= [0xF 0 0xA] w3-w1-w0) [:mov-wait-key vx]
            (= [0xF 1 5] w3-w1-w0) [:set-delay-timer vx]
            (= [0xF 1 8] w3-w1-w0) [:set-sound-timer vx]
            (= [0xF 1 0xE] w3-w1-w0) [:add-ip vx]
            (= [0xF 2 9] w3-w1-w0) [:set-font-ip vx]
            (= [0xF 3 3] w3-w1-w0) [:set-ip-decimal vx]
            (= [0xF 5 5] w3-w1-w0) [:set-memory vx]
            (= [0xF 6 5] w3-w1-w0) [:set-registers vx]))))

(defn inc-pc [machine] (update machine :PC + 2))
(defn set-pc [value-fn] (fn [machine] (assoc machine :PC (value-fn machine))))
(defn set-pc-const [const] (set-pc (constantly const)))
(defn reset-screen-memory [machine] (assoc machine :screen-memory 0))
(defn push-stack [machine] (update machine :stack conj (+ 2 (:PC machine))))
(defn pop-stack [machine] (update machine :stack pop))
(defn set-i [const] (fn [machine] (assoc machine :I const)))
(defn set-vx [vx const] (fn [machine] (update machine :registers assoc vx const)))

(defn get-vx [machine vx] (get-in machine [:registers vx]))

(defmulti command first)
(defmethod command :halt [_] (constantly nil))
(defmethod command :clear-screen [_] (comp inc-pc reset-screen-memory))
(defmethod command :return [_] (comp pop-stack (set-pc (fn [m] (peek (:stack m))))))
(defmethod command :sys [_] inc-pc)
(defmethod command :jump [[_ address]] (set-pc-const address))
(defmethod command :call [[_ address]] (comp (set-pc-const address) push-stack))
(defmethod command :mov-i [[_ address]] (comp inc-pc (set-i address)))
(defmethod command :skip-if-value [[_ vx test-op const]]
  (fn [machine]
    (if ((resolve test-op) (get-vx machine vx) const)
      (-> (inc-pc machine) inc-pc)
      (inc-pc machine))))
(defmethod command :mov-value [[_ vx nn]] (comp inc-pc (set-vx vx nn)))

(defn load-program [machine program]
  (-> (assoc machine :RAM (concat interpreter-code program)) ;; TODO 0 padding ?
      (assoc :PC 0x200)))

(defn read-opcode
  "returns a 2 bytes number at program counter"
  [machine]
  (bit-or
    (bit-shift-left (nth (:RAM machine) (:PC machine)) 8)
    (nth (:RAM machine) (inc (:PC machine)))))

(defn print-screen! [machine] (println "print screen"))

(defn start-machine [program]
  (let [machine (load-program fresh-machine program)]
    (loop [machine machine]
      (print-screen! machine)
      (println "@" (:PC machine))
      (let [opcode              (debug "opcode:" (read-opcode machine))
            instruction         (debug "instruction:" (opcode->instruction opcode))
            execute-instruction (command instruction)
            new-machine         (execute-instruction machine)]
        (if new-machine (recur new-machine) machine)))))

#_(start-machine [0x00E0])