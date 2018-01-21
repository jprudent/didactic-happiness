(ns repicene.debug
  (:require [clojure.core.async :refer [go >! <!! >!! poll!]]
            [repicene.decoder :refer [exec isize print-assembly decoder pc sp hex16 dword-at %16+ instruction-at-pc]]
            [repicene.schema :as s]
            [repicene.cpu :as cpu]
            [repicene.cpu-protocol :as cpu-protocol]
            [repicene.history :as history]
            [clojure.core.async :as async]))



(def breakpoint-opcodes
  {:permanent-breakpoint 0xD3
   :once-breakpoint      0xE3})

(defn add-x-breakpoint [cpu address breakpoint]
  {:pre  [(s/cpu? cpu)
          (s/address? address)
          (s/x-breakpoint? breakpoint)]
   :post [(s/cpu? %)]}
  (println "Set x-breakpoint at " address)
  (update cpu :x-breakpoints assoc address breakpoint))

(defn set-breakpoint
  [cpu address kind]
  {:pre  [(s/cpu? cpu) (s/address? address)]
   :post [(s/cpu? cpu)]}
  (let [original (cpu-protocol/word-at cpu address)]
    (println "original" original "op" (breakpoint-opcodes kind ))
    (-> (cpu-protocol/set-word-at cpu address (breakpoint-opcodes kind))
        (add-x-breakpoint address [original kind]))))                           ;; if memory region is written we override it, todo if we try to read it, we are screwed

(defn remove-breakpoint [{:keys [x-breakpoints] :as cpu}]
  {:pre  [(s/cpu? cpu) (get x-breakpoints (pc cpu))]
   :post [(s/cpu? cpu)]}
  (let [address (pc cpu)
        [original _] (get x-breakpoints address)]
    (-> (cpu-protocol/set-word-at cpu address original)
        (update :x-breakpoints dissoc address))))

(defn stop-debugging [cpu]
  (assoc cpu :mode ::s/running))

(defn- ->response [command response]
  {:command command :response response})

;; todo maybe that could be a map
(defmulti
  handle-debug-command
  "Handle a debug command. Returns a vector of 2 functions that takes a
    gameboy as parameter. The first one will be the next state of the gameboy.
    The second one will be the response sent to the client."
  (fn [command]
    (if (sequential? command)
      (first command)
      command)))

(defn decode [cpu address]
  (let [instruction (nth decoder (cpu-protocol/word-at cpu address))]
    [(print-assembly instruction cpu) (isize instruction)]))

(defn decode-from
  ([cpu] (decode-from cpu (pc cpu)))
  ([cpu address]
   {:pre [(s/address? address) (s/cpu? cpu)]}
   (lazy-seq
     (let [cpu     (pc cpu address)
           [instr-str size] (decode cpu address)
           bytes   (map (fn [relative-offset]
                          (cpu-protocol/word-at cpu (%16+ relative-offset (pc cpu))))
                        (range 0 size))
           next-pc (mod (+ size address) 0x10000)]
       (cons [address bytes instr-str]
             (decode-from cpu next-pc))))))

(defn- debug-view [gameboy]
  (select-keys gameboy [:registers :x-breakpoints]))

(defn- memory-dump
  ([cpu start end]
   (map (partial memory-dump cpu) (range start end 2)))
  ([cpu address]
   [address (dword-at cpu address)]))

(defn dump-region [cpu [start end]]
  [start end (memory-dump cpu start end)])

(defn ->debug-view [{:keys [regions]} cpu]
  (merge (debug-view cpu)
         {:instructions (take 10 (decode-from cpu))}
         (when regions {:regions (map (partial dump-region cpu) regions)})))

(defmethod handle-debug-command ::s/inspect
  [arg]
  (let [options (or (when (sequential? arg) (second arg)) {})]
    [identity (partial ->debug-view options)]))

(defmethod handle-debug-command ::s/kill
  [_]
  [(fn [{:keys [history-chan debug-chan-rx debug-chan-tx ] :as cpu}]
     (throw (ex-info "killing the machine"
                       {:command _
                        :clock (:clock cpu)
                        :signal  :kill})))
   identity])

(defmethod handle-debug-command ::s/resume
  [_]
  [stop-debugging
   (constantly :running)])

(defmethod handle-debug-command ::s/step-into
  [_]
  [cpu/cpu-cycle
   (partial ->debug-view {})])

(defmethod handle-debug-command :back-step
  [_]
  [history/restore! (partial ->debug-view {})])

(defn call? [instr] (= "Call" (.getSimpleName (class instr))))

(defn run-at [cpu target-pc]
  (if (= target-pc (pc cpu))
    cpu
    (recur (cpu/cpu-cycle cpu) target-pc)))

(defmethod handle-debug-command ::s/step-over
  [_]
  [(fn [cpu]
     (let [instruction (instruction-at-pc cpu)
           next-pc     (if (call? instruction)
                         (%16+ (pc cpu) (isize instruction))
                         (pc (exec instruction cpu)))]
       (println "Running at" next-pc)
       (run-at cpu next-pc)))
   (partial ->debug-view {})])

(defmethod handle-debug-command :add-breakpoint
  [[_ address]]
  [#(update % :x-breakpoints conj address)
   (partial ->debug-view {})])

(defmethod handle-debug-command :remove-breakpoint
  [[_ address]]
  [#(update % :x-breakpoints disj address)
   (partial ->debug-view {})])

(defmethod handle-debug-command ::s/return
  [_]
  [(fn [cpu]
     (let [ret (dword-at cpu (sp cpu))]
       (println "ret addr" ret)
       (run-at cpu ret)))
   (partial ->debug-view {})])

(defmethod handle-debug-command :default
  [command]
  [identity (constantly (do (println "unknown command" command)
                            "J'aime faire des craquettes au chien"))])

(defn process-debug-command
  [{:keys [debug-chan-tx] :as cpu} command]
  {:pre [(s/command? command) (s/cpu? cpu)]}
  (println "processing dbg comman" command)
  (let [[modify-cpu-fn response-fn] (handle-debug-command command)
        new-cpu  (modify-cpu-fn cpu)
        response (response-fn new-cpu)]
    (println "sending response")
    (>!! debug-chan-tx (->response command response))
    (println "response sent")
    new-cpu))

(defn send-break [ch]
  (println "sending :break")
  (>!! ch {:command :break})
  (println "sent :break"))

(defn debugging-loop [{:keys [debug-chan-rx mode] :as cpu}]
  (prn "debugging loop")
  (if (= ::s/debugging mode)
    (->> (<!! debug-chan-rx)
         (process-debug-command cpu)
         (recur))
    cpu))

(defn wait-ack [{:keys [debug-chan-tx debug-chan-rx]}]
  (send-break debug-chan-tx)
  (while (not= ::s/ack-break (<!! debug-chan-rx))
    (send-break debug-chan-tx)))

(defn process-breakpoint [cpu]
  (wait-ack cpu)
  (debugging-loop (assoc cpu :mode ::s/debugging)))