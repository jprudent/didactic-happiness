(ns cheap-hate.core)

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

(def ^:static interpreter-code (concat fonts (repeat (- 0x200 (count fonts)) 0)))

;; A fresh machine craving for a program to run
(def ^:static empty-screen (vec (for [_ (range 32)] (vec (repeat 64 0)))))
(def ^:static fresh-machine {:RAM         (vec (concat interpreter-code
                                                       (repeat (- 0x1000 0x200) 0)))
                             :registers   (vec (repeat 16 0))
                             :I           0
                             :PC          0
                             :stack       []
                             :screen      empty-screen
                             :delay-timer 0
                             :sound-timer 0
                             :prn         42})

(def keyboard-device (atom nil))

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
(defn bit-at [bit-num byte] (nth-word 1 (- 7 bit-num) byte))

(defn opcode->instruction
  "extract informations from a 16 bits big-endian opcode."
  [opcode]
  (let [[w3 _ w0 :as w3-w1-w0] (w3-w1-w0 opcode)
        w3-w0 [w3 w0]]
    (mapv #(if (fn? %) (% opcode) %)
          (cond
            (= opcode 0) [:halt]                                                ;; this one, I made up for testing purpose
            (= opcode 0x00E0) [:clear-screen]
            (= opcode 0x00EE) [:return]
            (= 0 w3) [:sys address]
            (= 1 w3) [:jump address]
            (= 2 w3) [:call address]
            (= 3 w3) [:skip-if-value vx '= nn]
            (= 4 w3) [:skip-if-value vx 'not= nn]
            (= [5 0] [w3 w0]) [:skip-if-register vx '= vy]
            (= 6 w3) [:mov-register-value vx nn]
            (= 7 w3) [:add-register-value vx nn]
            (= [8 0] w3-w0) [:mov-register-register vx vy]
            (= [8 1] w3-w0) [:or-register-register vx vy]
            (= [8 2] w3-w0) [:and-register-register vx vy]
            (= [8 3] w3-w0) [:xor-register-register vx vy]
            (= [8 4] w3-w0) [:add-register-register vx vy]
            (= [8 5] w3-w0) [:sub-register-register vx vy]
            (= [8 6] w3-w0) [:shift-right-register vx]
            (= [8 7] w3-w0) [:sub-reverse-register-register vx vy]
            (= [8 0xE] w3-w0) [:shift-left-register vx]
            (= [9 0] w3-w0) [:skip-if-register vx 'not= vy]
            (= 0xA w3) [:mov-i address]
            (= 0xB w3) [:jmp-add-pc-v0 address]
            (= 0xC w3) [:mov-register-random vx nn]
            (= [0xF 1 8] w3-w1-w0) [:mov-sound-timer vx]
            (= [0xF 1 0xE] w3-w1-w0) [:add-i vx]
            (= [0xF 2 9] w3-w1-w0) [:mov-i-font vx]
            (= [0xF 3 3] w3-w1-w0) [:mov-i-decimal vx]
            (= 0xD w3) [:draw vx vy height]
            (= [0xE 9 0xE] w3-w1-w0) [:skip-if-key '= vx]
            (= [0xE 0xA 1] w3-w1-w0) [:skip-if-key 'not= vx]
            (= [0xF 0 7] w3-w1-w0) [:mov-register-delay-timer vx]
            (= [0xF 0 0xA] w3-w1-w0) [:mov-register-key vx]
            (= [0xF 1 5] w3-w1-w0) [:mov-delay-timer vx]
            (= [0xF 5 5] w3-w1-w0) [:set-memory vx]
            (= [0xF 6 5] w3-w1-w0) [:mov-registers-memory vx]))))

(defn inc-pc [machine] (update machine :PC + 2))
(defn set-pc [machine address] (assoc machine :PC address))
(defn reset-screen-memory [machine] (assoc machine :screen empty-screen))
(defn push-stack [machine] (update machine :stack conj (+ 2 (:PC machine))))
(defn pop-stack [machine] (update machine :stack pop))
(defn assoc-i [machine nnn] (assoc machine :I nnn))
(defn update-i [machine f] (update machine :I f))
(defn update-register [machine x update-vx] (update machine :registers update x update-vx))
(defn set-registers
  ([machine x v & others] (apply update machine :registers assoc x v others))
  ([[machine & registers]] (apply set-registers machine registers)))
(defn set-delay-timer [v machine] (assoc machine :delay-timer v))
(defn set-sound-timer [v machine] (assoc machine :sound-timer v))
(defn get-register [machine x] (get-in machine [:registers x]))
(defn get-registers [machine & registers]
  (apply vector machine
         (map (juxt identity (partial get-register machine)) registers)))
(defn next-int [old-seed] (lowest-byte (+ 3 old-seed)))                         ;; TODO have a proper prng
(defn update-prng [machine] (update machine :prn next-int))
(defn get-prng [machine] (get machine :prn))
(defn get-i [machine] (get machine :I))
(defn set-mem [machine address values]
  (loop [machine machine
         address address
         values  values]
    (if (empty? values)
      machine
      (recur (update machine :RAM assoc address (first values))
             (inc address)
             (rest values)))))
(defn read-memory [machine address n]
  (subvec (:RAM machine) address (+ address n)))
(defn get-pixel [machine x y]
  (get-in machine [:screen y x]))
(defn get-delay-timer [machine]
  (get machine :delay-timer))
(defn peek-stack [machine]
  (peek (:stack machine)))
(defn pressed-key [] @keyboard-device)

(defmulti execute (fn [_ instruction] (first instruction)))

;; Misc instructions

(defmethod execute :halt [_ _] nil)

(defmethod execute :sys [machine _]
  (inc-pc machine))


;; Instructions that change the I register

(defmethod execute :mov-i [machine [_ address]]
  (-> (assoc-i machine address)
      inc-pc))

(defn add [nn] (comp lowest-byte (partial + nn)))

(defmethod execute :add-i [machine [_ x]]
  (letfn [(call-update-i [[machine [_ vx]]] (update-i machine (add vx)))]
    (-> (get-registers machine x)
        call-update-i
        inc-pc)))

(defmethod execute :mov-i-decimal [machine [_ x]]
  (letfn [(call-set-mem [[machine [_ vx]]]
            (set-mem machine (get-i machine)
                     (map #(Integer/valueOf (str %)) (str vx))))]
    (-> (get-registers machine x)
        call-set-mem
        inc-pc)))

(defmethod execute :mov-i-font [machine [_ sprite]]
  (-> (assoc-i machine (* sprite 5))
      inc-pc))


;; Jump instructions

(defmethod execute :call [machine [_ address]]
  (-> (push-stack machine)
      (set-pc address)))

(defmethod execute :return [machine _]
  (-> (set-pc machine (peek-stack machine))
      pop-stack))

(defmethod execute :jump [machine [_ address]]
  (set-pc machine address))

(letfn [(skip-if [machine op x y]
          (-> (if (op x y) (inc-pc machine) machine)
              inc-pc))]

  (defmethod execute :skip-if-value [machine [_ x test-op nn]]
    (skip-if machine (resolve test-op) (get-register machine x) nn))

  (defmethod execute :skip-if-register [machine [_ x test-op y]]
    (skip-if machine (resolve test-op) (get-register machine x) (get-register machine y))))

(defmethod execute :jmp-add-pc-v0 [machine [_ addr]]
  (set-pc machine (+ (get-register machine 0) addr)))

;; Instructions that change registers

(defmethod execute :mov-register-value [machine [_ x nn]]
  (-> (set-registers machine x nn)
      inc-pc))

(defmethod execute :mov-register-register [machine [_ x y]]
  (letfn [(switch-vals [[machine [x _] [_ vy]]] [machine x vy])]
    (-> (get-registers machine x y)
        switch-vals
        set-registers
        inc-pc)))

(defmethod execute :add-register-value [machine [_ x nn]]
  (-> (update-register machine x (add nn))
      inc-pc))

(defmethod execute :mov-register-delay-timer [machine [_ x]]
  (-> (set-registers [machine x (get-delay-timer machine)])
      inc-pc))

(defmethod execute :mov-register-random [machine [_ x nn]]
  (letfn [(random [machine] [machine x (bit-and nn (get-prng machine))])]
    (-> (update-prng machine)
        random
        set-registers
        inc-pc)))

(defmethod execute :mov-register-key [machine [_ x]]                            ;; This implementation will loop like hell til no key is pressed
  (if-let [key (pressed-key)]
    (inc-pc (set-registers [machine x key]))
    machine))

(defmethod execute :mov-registers-memory [machine [_ n]]
  (-> (apply set-registers machine
             (interleave (range) (read-memory machine (get-i machine) n)))
      inc-pc))

(letfn [(arithmetic [apply-op machine x y]
          (-> (get-registers machine x y)
              apply-op
              set-registers
              inc-pc))]

  (letfn [(arithmetic-with-carry [machine x y op carry?]
            (letfn [(apply-op [[machine [x vx] [_ vy]]]
                      (let [r (op vx vy)]
                        [machine, x (lowest-byte r), 0xF (if (carry? r) 1 0)]))]
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
            (-> (get-registers machine x)
                shift
                set-registers
                inc-pc)))]

  (defmethod execute :shift-right-register [machine [_ x]]
    (shift machine x bit-shift-right lowest-bit))

  (defmethod execute :shift-left-register [machine [_ x]]
    (shift machine x (comp lowest-byte bit-shift-left) highest-bit)))


;; Instructions that change timers

(letfn [(set-timer [machine x set-x-timer]
          (letfn [(call-set-timer [[machine [_ x]]] (set-x-timer x machine))]
            (-> (get-registers machine x)
                call-set-timer
                inc-pc)))]

  (defmethod execute :mov-sound-timer [machine [_ x]]
    (set-timer machine x set-sound-timer))

  (defmethod execute :mov-delay-timer [machine [_ x]]
    (set-timer machine x set-delay-timer)))


;; Instructions that update the screen memory

(defmethod execute :clear-screen [machine _]
  (-> (reset-screen-memory machine)
      inc-pc))

(letfn [(one-byte-sprite-bits [one-byte-sprite]
          (map (fn [bit-num] (bit-at bit-num one-byte-sprite)) (range 8)))

        (print-pixel [machine x y pixel]
          (let [real-x       (mod x 64)
                real-y       (mod y 32)
                actual-pixel (get-pixel machine real-x real-y)]
            (if (= actual-pixel pixel)
              machine
              (-> (update machine :registers assoc 0xF 1)
                  (assoc-in [:screen real-y real-x] pixel)))))

        (print-1-byte-sprite [machine [x y one-byte-sprite]]
          (reduce (fn [machine [bit-num pixel]] (print-pixel machine (+ x bit-num) y pixel))
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
      (-> (get-registers machine x y)
          draw
          inc-pc))))


;; Instructions that change memory

(letfn [(call-set-mem [[machine & registers]]
          (set-mem machine (get-i machine) (map second registers)))]

  (defmethod execute :set-memory [machine [_ x]]
    (-> (apply get-registers machine (range 0 (inc x)))
        call-set-mem
        inc-pc)))


(defn load-program [machine program]
  (let [used-mem (concat interpreter-code program)
        padding  (repeat (- 0x1000 (count used-mem)) 0)                         ;; The uppermost region of memory is padded with 0
        mem      (vec (concat used-mem padding))]
    (-> (assoc machine :RAM mem)
        (assoc :PC 0x200))))

(defn concat-bytes [b1 b2] (bit-or (bit-shift-left b1 8) b2))
(defn byte-at-pc [machine pc-fn] (nth (:RAM machine) (pc-fn (:PC machine))))
(defn read-opcode
  "returns a 2 bytes number at program counter"
  [machine]
  (concat-bytes
    (byte-at-pc machine identity) (byte-at-pc machine inc)))

(defprotocol Screen
  (print-screen [this machine]))

(defn start-machine [screen program]
  (let [machine (load-program fresh-machine program)]
    (loop [machine machine]
      (print-screen screen machine)
      (let [opcode      (read-opcode machine)
            instruction (opcode->instruction opcode)
            new-machine (execute machine instruction)]
        (if new-machine (recur new-machine) machine)))))                        ;; new-machine is nil when opcode = 0 (see :halt)
