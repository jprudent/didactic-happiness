(ns cheap-hate.core)

(defprotocol UpdatableMachine
  "A machine that can be updated"
  (load-program [machine program]
    "Load the program in memory. Machine is ready to run the program.
    The program counter should be set to the address of the first instruction.")
  (inc-pc [machine]
    "Increment the program counter to the next instruction.")
  (assoc-pc [machine address]
    "Associate the program counter to an arbitrary address.
    0 <= Address < 0x1000")
  (push-stack [machine]
    "Save the current value of program counter in the stack frame.")
  (pop-stack [machine]
    "Pop the stack frame")
  (assoc-i [machine nnn]
    "Associate an arbitrary 16 bits number to register I.
    0 <= nnn <= 0xFFF")
  (update-i [machine f]
    "Update the current value of register I with the result of f applied to
    the current value of register I.
    The result of f(I) is trucated to 16bits number.")
  (update-register [machine x f]
    "Update the current value of regixter Vx with the result of f applied to
    the current value of register Vx.
    f(Vx) is truncated to 8 bits")
  (assoc-registers [machine registers]
    "Assoc registers to values.
    registers is a seq like [[X VX] [Y VY] ...] It assoc VX to register X.
    0 <= value <= 0xFF")
  (assoc-delay-timer [machine value]
    "Assoc value to the delay timer.
    0 <= value <= 0xFF")
  (update-delay-timer [machine f]
      "Apply f to the value of the delay timer.
      0 <= value <= 0xFF")
  (assoc-sound-timer [machine value]
    "Assoc value to the sound timer.
    0 <= f(timer) <= 0xFF")
  (update-sound-timer [machine f]
      "Apply f to the value of the sound timer.
      0 <= f(timer) <= 0xFF")
  (update-prng [machine]
    "Update the pseudo random number generator to its next value")
  (assoc-mem [machine address values]
    "Assoc values to memory at address.
    values is a seq of 8 bits numbers.
    0 <= address <= 0xFFF")
  (reset-screen-memory [machine]
    "Assoc every pixels to 0")
  (set-pixel [machine x y]
    "Set the pixel in memory screen at coors (x,y)
    0 <= x < 64
    0 <= y < 32")
  (unset-pixel [machine x y]
    "Unset the pixel in memory screen at coors (x,y)
    0 <= x < 64
    0 <= y < 32")
  (assoc-keyboard [machine key]
    "When a key is physically pressed on keyboard, the main loop should update
    the keyboard register with the pressed key.
    Assoc key to the keyboard register.
    0 <= key <= 15 or key = nil"))

(defprotocol InspectableMachine
  "A machine that exposes its state"
  (get-register [machine x]
    "Get the value of the x register. It's an 8 bits number.
    0 <= x <= 15")
  (get-prn [machine]
    "Get the value of the pseudo random number. It's an 8 bits number.")
  (get-i [machine]
    "Get the value of the I register. It's a 16 bits number")
  (read-memory [machine address size]
    "Returns a seq of `size` bytes representing the memory at `address`
    0 <= address <= 0xFFF
    0 <= size <= 0xFFF")
  (get-pc [machine]
    "Get the value of the program counter register.
    The PC is a number between 0 and 0xFFF.")
  (get-pixel [machine x y]
    "Get the pixel state at (x,y).
    It returns 0 if pixel is unset, 1 otherwise")
  (get-delay-timer [machine]
    "Get the value of the delay timer. It's an 8 bits number.")
  (peek-stack [machine]
    "Get the address on top of the stack frame.
    The address is a number between 0 and 0xFFF.")
  (get-keyboard [machine]
    "Get the 4 bits number in the keyboard register.
    Can be nil if no key is pressed."))


(defprotocol RunnableMachine
  "A machine that can run programs."
  (start-machine [machine opts]
    "This function will loop forever until the machine halts."))


(defprotocol Screen
  (print-screen [this machine last-instruction]
    "Given a machine, print the corresponding screen.
    last-instruction is given as a hint for optimization."))


(defprotocol FlightRecorder
  "Log the state of a machine"
  (record [this machine opcode]
    "Record the execution of opcode that resulted in machine"))


(defprotocol Keyboard
  "Read the keyboard"
  (read-device [this]
    "update the keyboard against physical state.
    returns an updated keyboard")
  (pressed-key [this]
    "returns the key currently pressed"))


(defprotocol Clock
  "The clock is used to adjust the speed of the machine."
  (throttle [this]
    "Calling this function will block until the desired delay has been reached.
    Returns an updated version of this."))