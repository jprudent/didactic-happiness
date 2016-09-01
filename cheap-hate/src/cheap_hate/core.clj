(ns cheap-hate.core)



(def ^:static MEMORY_SIZE 0x1000)
(def empty-memory (vec (repeat MEMORY_SIZE 0)))

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

(def head (partial nth-word 4 3))
(def tail (partial nth-word 4 0))
(def ttail (partial nth-word 4 1))
(defn head-tail [opcode] [(head opcode) (tail opcode)])
(defn head-ttail [opcode] [(head opcode) (ttail opcode) (tail opcode)])
(def address (partial nth-word 12 0))
(def vx (partial nth-word 4 2))
(def vy (partial nth-word 4 1))
(def value (partial nth-word 8 0))
(def height (partial nth-word 4 0))

(defn extract-opcode
  "given an opcode extract information"
  [opcode]
  (cond
    (= opcode 0x00E0) [:clear-screen]
    (= opcode 0x00EE) [:return]
    (= 0 (head opcode)) [:RCA-1802]
    (= 1 (head opcode)) [:jmp (address opcode)]
    (= 2 (head opcode)) [:call (address opcode)]
    (= 3 (head opcode)) [:skip-if (vx opcode) = (value opcode)]
    (= 4 (head opcode)) [:skip-if (vx opcode) not= (value opcode)]
    (= [5 0] (head-tail opcode)) [:skip-if (vx opcode) = (vy opcode)]
    (= 6 (head opcode)) [:mov-value (vx opcode) (value opcode)]
    (= 7 (head opcode)) [:add-value (vx opcode) (value opcode)]
    (= [8 0] (head-tail opcode)) [:mov-register (vx opcode) (vy opcode)]
    (= [8 1] (head-tail opcode)) [:or (vx opcode) (vy opcode)]
    (= [8 2] (head-tail opcode)) [:and (vx opcode) (vy opcode)]
    (= [8 3] (head-tail opcode)) [:xor (vx opcode) (vy opcode)]
    (= [8 4] (head-tail opcode)) [:add-register (vx opcode) (vy opcode)]
    (= [8 5] (head-tail opcode)) [:sub-register (vx opcode) (vy opcode)]
    (= [8 6] (head-tail opcode)) [:shr (vx opcode)]
    (= [8 7] (head-tail opcode)) [:sub-reverse-register (vx opcode) (vy opcode)]
    (= [8 0xE] (head-tail opcode)) [:shl (vx opcode)]
    (= [9 0] (head-tail opcode)) [:skip-if (vx opcode) not= (vy opcode)]
    (= 0xA (head opcode)) [:mov-ip (address opcode)]
    (= 0xB (head opcode)) [:jmp-add-v0 (address opcode)]
    (= 0xC (head opcode)) [:random (vx opcode) (value opcode)]
    (= 0xD (head opcode)) [:draw (vx opcode) (vy opcode) (height opcode)]
    (= [0xE 9 0xE] (head-ttail opcode)) [:skip-if-key = (vx opcode)]
    (= [0xE 0xA 1] (head-ttail opcode)) [:skip-if-key not= (vx opcode)]
    (= [0xF 0 7] (head-ttail opcode)) [:mov-timer (vx opcode)]
    (= [0xF 0 0xA] (head-ttail opcode)) [:mov-wait-key (vx opcode)]
    (= [0xF 1 5] (head-ttail opcode)) [:set-delay-timer (vx opcode)]
    (= [0xF 1 8] (head-ttail opcode)) [:set-sound-timer (vx opcode)]
    (= [0xF 1 0xE] (head-ttail opcode)) [:add-ip (vx opcode)]
    (= [0xF 2 9] (head-ttail opcode)) [:set-font-ip (vx opcode)]
    (= [0xF 3 3] (head-ttail opcode)) [:set-ip-decimal (vx opcode)]
    (= [0xF 5 5] (head-ttail opcode)) [:set-memory (vx opcode)]
    (= [0xF 6 5] (head-ttail opcode)) [:set-registers (vx opcode)]))

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

(defn decode-opcode
  "decode an opcode and return a function to apply on the machine"
  [opcode]
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