(ns repicene.core
  (:require [repicene.file-loader :refer [load-rom]]
            [repicene.debug :refer [process-debug-command process-breakpoint set-breakpoint]]
            [repicene.decoder :refer [exec pc fetch decoder hex16]]
            [clojure.core.async :refer [sliding-buffer go >! chan poll! <!! thread]]
            [repicene.schema :as s]
            [repicene.cpu :refer [cpu-cycle]]))

(def ^byte cell 0)
(defn new-cpu [rom]
  {:pre [(= 0x8000 (count rom))]}
  {::s/registers          {::s/AF 0
                           ::s/BC 0
                           ::s/DE 0
                           ::s/HL 0
                           ::s/SP 0
                           ::s/PC 0}
   ::s/interrupt-enabled? true
   ::s/memory             (vec (concat rom                                      ;; rom
                                       (repeat 0x2000 cell)                     ;; vram
                                       (repeat 0x2000 cell)                     ;; ext-ram
                                       (repeat 0x1000 cell)                     ;; wram-0
                                       (repeat 0x1000 cell)                     ;; wram-1
                                       (repeat 0x1E00 cell)                     ;; echo
                                       (repeat 0x00A0 cell)                     ;; oam-ram
                                       (repeat 0x0060 cell)                     ;; unusable
                                       (repeat 0x0080 cell)                     ;; io
                                       (repeat 0x0080 cell)))                   ;; hram
   ::s/mode               ::s/running
   :debug-chan-rx         (chan)
   :debug-chan-tx         (chan)
   :history-chan          (chan (sliding-buffer 100))
   ::s/x-breakpoints      {}
   :w-breakpoints         {}
   :debugging?            nil})

(defn halted? [{:keys [::s/mode]}] (= ::s/halted mode))
(defn cpu-loop [{:keys [debug-chan-rx] :as cpu}]
  {:pre [(s/valid? cpu)]}
  (let [command (poll! debug-chan-rx)]
    (if (halted? cpu)
      cpu
      (recur
        (cond-> cpu
                command (process-debug-command command)
                (:break? cpu) (process-breakpoint)
                :always (cpu-cycle))))))

(defn demo-gameboy
  ([]
   (->
     (vec (take 0x8000 (load-rom "roms/cpu_instrs/individual/07-jr,jp,call,ret,rst.gb")))
     (new-cpu)
     (pc 0x100)
     (set-breakpoint 0x0100 :permanent-breakpoint)
     (set-breakpoint 0xFF01 :permanent-breakpoint)))
  ([coredump]
   (let [gameboy (-> (demo-gameboy)
                     (merge (read-string (slurp coredump))))]
     (set-breakpoint gameboy (pc gameboy) :permanent-breakpoint))))