(ns cheap-hate.core)



(def ^:static MEMORY_SIZE 0x1000)
(def fresh-memory (vec (repeat MEMORY_SIZE 0)))
(def fresh-screen-memory )
(def fresh-machine {} #_{:memory        empty-memory
                         :registers     {:V0 0, :V1 0, :V2 0, :V3 0
                                         :V4 0, :V5 0, :V6 0, :V7 0
                                         :V8 0, :V9 0, :V10 0, :V11 0}
                         :screen-memory 0})


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

(defn extract-opcode
  "given an opcode extract information"
  [opcode]
  (let [[w3 _ w0 :as w3-w1-w0] (w3-w1-w0 opcode)
        w3-w0 [w3 w0]]
    (cond
      (= opcode 0x00E0) [:clear-screen]
      (= opcode 0x00EE) [:return]
      (= 0 w3) [:RCA-1802]
      (= 1 w3) [:jmp address]
      (= 2 w3) [:call address]
      (= 3 w3) [:skip-if vx = nn]
      (= 4 w3) [:skip-if vx not= nn]
      (= [5 0] [w3 w0]) [:skip-if vx = vy]
      (= 6 w3) [:mov-value vx nn]
      (= 7 w3) [:add-value vx nn]
      (= [8 0] w3-w0) [:mov-register vx vy]
      (= [8 1] w3-w0) [:or vx vy]
      (= [8 2] w3-w0) [:and vx vy]
      (= [8 3] w3-w0) [:xor vx vy]
      (= [8 4] w3-w0) [:add-register vx vy]
      (= [8 5] w3-w0) [:sub-register vx vy]
      (= [8 6] w3-w0) [:shr vx]
      (= [8 7] w3-w0) [:sub-reverse-register vx vy]
      (= [8 0xE] w3-w0) [:shl vx]
      (= [9 0] w3-w0) [:skip-if vx not= vy]
      (= 0xA w3) [:mov-ip address]
      (= 0xB w3) [:jmp-add-v0 address]
      (= 0xC w3) [:random vx nn]
      (= 0xD w3) [:draw vx vy height]
      (= [0xE 9 0xE] w3-w1-w0) [:skip-if-key = vx]
      (= [0xE 0xA 1] w3-w1-w0) [:skip-if-key not= vx]
      (= [0xF 0 7] w3-w1-w0) [:mov-timer vx]
      (= [0xF 0 0xA] w3-w1-w0) [:mov-wait-key vx]
      (= [0xF 1 5] w3-w1-w0) [:set-delay-timer vx]
      (= [0xF 1 8] w3-w1-w0) [:set-sound-timer vx]
      (= [0xF 1 0xE] w3-w1-w0) [:add-ip vx]
      (= [0xF 2 9] w3-w1-w0) [:set-font-ip vx]
      (= [0xF 3 3] w3-w1-w0) [:set-ip-decimal vx]
      (= [0xF 5 5] w3-w1-w0) [:set-memory vx]
      (= [0xF 6 5] w3-w1-w0) [:set-registers vx])))

(def opcode-mapping
  {:0NNN "Calls RCA 1802 program at address NNN. Not necessary for most ROMs."
   :00E0 "Clears the screen."
   :00EE "Returns from a subroutine."
   :1NNN "Jumps to address NNN."
   :2NNN "Calls subroutine at NNN."
   :3XNN "Skips the next instruction if VX equals NN."
   :4XNN "Skips the next instruction if VX doesn't equal NN."
   :5XY0 "Skips the next instruction if VX equals VY."
   :6XNN "Sets VX to NN."
   :7XNN "Adds NN to VX."
   :8XY0 "Sets VX to the value of VY."
   :8XY1 "Sets VX to VX or VY."
   :8XY2 "Sets VX to VX and VY."
   :8XY3 "Sets VX to VX xor VY."
   :8XY4 "Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't."
   :8XY5 "VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't."
   :8XY6 "Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift.[2]"
   :8XY7 "Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't."
   :8XYE "Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift.[2]"
   :9XY0 "Skips the next instruction if VX doesn't equal VY."
   :ANNN "Sets I to the address NNN."
   :BNNN "Jumps to the address NNN plus V0."
   :CXNN "Sets VX to the result of a bitwise and operation on a random number and NN."
   :DXYN "Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels. Each row of 8 pixels is read as bit-coded starting from memory location I; I value doesn’t change after the execution of this instruction. As described above, VF is set to 1 if any screen pixels are flipped from set to unset when the sprite is drawn, and to 0 if that doesn’t happen"
   :EX9E "Skips the next instruction if the key stored in VX is pressed."
   :EXA1 "Skips the next instruction if the key stored in VX isn't pressed."
   :FX07 "Sets VX to the value of the delay timer."
   :FX0A "A key press is awaited, and then stored in VX."
   :FX15 "Sets the delay timer to VX."
   :FX18 "Sets the sound timer to VX."
   :FX1E "Adds VX to I.[3]"
   :FX29 "Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are represented by a 4x5 font."
   :FX33 "Stores the binary-coded decimal representation of VX, with the most significant of three digits at the address in I, the middle digit at I plus 1, and the least significant digit at I plus 2. (In other words, take the decimal representation of VX, place the hundreds digit in memory at location in I, the tens digit at location I+1, and the ones digit at location I+2.)"
   :FX55 "Stores V0 to VX (including VX) in memory starting at address I.[4]"
   :FX65 "Fills V0 to VX (including VX) with values from memory starting at address I.[4]"})

(defmulti command first)

(defmethod command :clear-screen [_]
  (fn [machine]
    (assoc machine :screen-memory fresh-screen-memory)))

(defn decode-opcode
  "decode an opcode and return a function to apply on the machine"
  [opcode]
  (extract-opcode opcode)
  (fn [machine]
    (println (get opcode-mapping opcode))
    machine))

(defn load-program [machine program]
  (-> machine
      (assoc-in [:memory] program)
      (assoc-in [:IP] 0 #_0x200)))

(defn read-opcode [machine]
  (->> (get (:memory machine) (:IP machine))
       (format "%04X")
       (keyword)))

(defn print-screen! [machine] (println "print screen"))

(defn read-input! [machine])

(defn start-machine [program]
  (let [machine (load-program fresh-machine program)]
    (loop [machine machine]
      (when machine
        (print-screen! machine)
        (recur (let [raw-opcode (read-opcode machine)
                     run-opcode (decode-opcode raw-opcode)]
                 (run-opcode machine))))))
  )

#_(start-machine [0x00E0])