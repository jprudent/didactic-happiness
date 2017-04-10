(ns repicene.debug
  (:require [clojure.core.async :refer [go >! <!! poll!]]
            [repicene.decoder :refer [decoder word-at pc sp unknown hex16 dword-at %16]]
            [repicene.schema :as s]
            [repicene.cpu :refer [cpu-cycle instruction-at-pc stop-debugging]]
            [repicene.history :as history]))

(defn- ->response [command response]
  {:command command :response response})

(defmulti
  handle-debug-command
  "Handle a debug command. Returns a vector of 2 functions that takes a
    gameboy as parameter. The first one will be the next state of the gameboy.
    The second one will be the response sent to the client."
  (fn [command]
    (if (sequential? command)
      (first command)
      command)))

(defn decode [{:keys [::s/memory] :as cpu} address]
  (let [{:keys [to-string size]} (or (decoder (word-at memory address)) unknown)]
    [(to-string cpu) size]))


(defn next-address-of-prev [cpu n]                                              ;; 150 ms =
  (let [cpu-0-pc      (pc cpu)                                                  ;; 20
        cpu-1-pc      (%16 - cpu-0-pc n)
        #__             #_(println "cpu-1-pc" (hex16 cpu-1-pc))
        cpu-1         (pc cpu cpu-1-pc)                                         ;; 40
        instruction-1 (instruction-at-pc cpu-1)]                                ;; 80
    #_(println (hex16 cpu-0-pc) n instruction-1)
    (%16 + cpu-1-pc (:size instruction-1))))

(defn prev-addrs
  ([cpu]
   (let [pc-cpu    (pc cpu)
         prev-addr (%16 -
                        pc-cpu
                        (or (first
                              (filter #(= pc-cpu (next-address-of-prev cpu %))
                                      (range 3 0 -1)))
                            1))]
     (lazy-seq
       (cons prev-addr
             (prev-addrs (pc cpu prev-addr)))))))

(defn decode-from
  ([cpu] (decode-from cpu (pc cpu) #_(nth (prev-addrs cpu) 3)))
  ([{:keys [::s/memory] :as cpu} address]
   {:pre [(s/address? address) (s/valid? cpu)]}
   (lazy-seq
     (let [cpu     (pc cpu address)
           [instr-str size] (decode cpu address)
           bytes   (map #(word-at memory (+ % (pc cpu))) (range 0 size))
           next-pc (mod (+ size address) 0x10000)]
       (cons [address bytes instr-str]
             (decode-from cpu next-pc))))))

(defn- debug-view [gameboy]
  (select-keys gameboy [::s/registers ::s/x-breakpoints]))

(defn- memory-dump
  ([cpu start end]
   (map (partial memory-dump cpu) (range start end 2)))
  ([cpu address]
   [address (dword-at cpu address)]))

(defn dump-region [cpu [start end]]
  [start end (memory-dump cpu start end)])

(defn ->debug-view [options cpu]
  (into (debug-view cpu)
        {:instructions (take 10 (decode-from cpu))
         :regions      (when-let [regions (:regions options)]
                         (map (partial dump-region cpu) regions))}))

(defmethod handle-debug-command :inspect
  [[_ options]]
  [identity (partial ->debug-view options)])

(defmethod handle-debug-command :kill
  [_]
  (throw (Exception. "Harakiri")))

(defmethod handle-debug-command :reset
  [_]
  [#(pc % 0x100) (constantly :ok)])

(defmethod handle-debug-command :resume
  [_]
  [stop-debugging (constantly :running)])

(defmethod handle-debug-command :step-into
  [_]
  [cpu-cycle (partial ->debug-view {})])

(defmethod handle-debug-command :back-step
  [_]
  [history/restore! (partial ->debug-view {})])

(defmethod handle-debug-command :step-over
  [_]
  [(fn [cpu]
     (let [{[kind & _] :asm size :size} (instruction-at-pc cpu)
           next-instr (+ (pc cpu) size)]
       (if (= :call kind)
         (-> (update cpu :x-once-breakpoints conj #(= (pc %) next-instr))
             (stop-debugging))
         (cpu-cycle cpu))))
   (fn [{:keys [debugging?] :as cpu}]
     (if debugging?
       (->debug-view {} cpu)
       :running))])

(defmethod handle-debug-command :add-breakpoint
  [[_ address]]
  [#(update % ::s/x-breakpoints conj address)
   (partial ->debug-view {})])

(defmethod handle-debug-command :remove-breakpoint
  [[_ address]]
  [#(update % ::s/x-breakpoints disj address)
   (partial ->debug-view {})])


(defmethod handle-debug-command :return
  [_]
  [(fn [cpu]
     (let [ret (dword-at cpu (sp cpu))]
       (println "ret addr" ret)
       (-> (update cpu :x-once-breakpoints conj #(= (pc %) ret))
           (stop-debugging))))
   (constantly :running)])

(defmethod handle-debug-command :default
  [command]
  [identity (constantly (do (println "unknown command" command)
                            "J'aime faire des craquettes au chien"))])

(defn process-debug-command
  [{:keys [debug-chan-tx] :as cpu} command]
  (let [[modify-cpu-fn response-fn] (handle-debug-command command)
        new-cpu  (modify-cpu-fn cpu)
        response (response-fn new-cpu)]
    (go (>! debug-chan-tx (->response command response)))
    new-cpu))

