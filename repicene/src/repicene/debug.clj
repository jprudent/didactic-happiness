(ns repicene.debug
  (:require [clojure.core.async :refer [go >! <!! >!! poll!]]
            [repicene.decoder :refer [decoder set-word-at word-at pc sp unknown hex16 dword-at %16 instruction-at-pc]]
            [repicene.schema :as s]
    #_[repicene.cpu :refer [cpu-cycle instruction-at-pc stop-debugging]]
            [repicene.history :as history]
            [repicene.instructions :refer [exec]]))

(defn set-w-breakpoint
  [cpu address hook]
  (update-in cpu [:w-breakpoints] assoc address hook))

(def breakpoint-opcodes
  {:permanent-breakpoint 0xD3
   :once-breakpoint      0xE3})

(defn add-breakpoint [cpu address breakpoint]
  {:pre  [(s/valid? cpu)
          (s/address? address)
          (s/x-breakpoint? breakpoint)]
   :post [(s/valid? %)]}
  (update-in cpu [::s/x-breakpoints] assoc address breakpoint))

(defn w-memory-hook [address kind]
  (fn [cpu val]
    (println "writing " val "at" address)
    (let [cpu (-> (update-in cpu [:w-breakpoints] dissoc address)               ;; remove write memory hook because ...
                  (set-word-at address (breakpoint-opcodes kind))               ;; we write at this exact same place
                  (add-breakpoint address [val kind])                           ;; overwrite the breakpoint with the new original value
                  (set-w-breakpoint address (w-memory-hook address kind)))]     ;; and set back the original write memory hook
      (println "breakpoints " (::s/x-breakpoints cpu))
      cpu)))

(defn set-breakpoint
  [{:keys [::s/memory] :as cpu} address kind]
  {:pre  [(s/valid? cpu) (s/address? address)]
   :post [(s/valid? cpu)]}
  (let [original (word-at memory address)]
    (-> (set-word-at cpu address (breakpoint-opcodes kind))
        (add-breakpoint address [original kind])
        (set-w-breakpoint address (w-memory-hook address kind)))))              ;; if memory region is written we override it, todo if we try to read it, we are screwed

(defn remove-breakpoint [{:keys [::s/x-breakpoints] :as cpu} address]
  {:pre  [(s/valid? cpu) (s/address? address)]
   :post [(s/valid? cpu)]}
  (let [[original _] (get x-breakpoints (pc cpu))]
    (set-word-at cpu (pc cpu) original)
    (update-in [::s/x-breakpoints] dissoc address)))

(defn after-break [{:keys [::s/x-breakpoints] :as cpu}]
  (let [[_ kind] (get x-breakpoints (pc cpu))]
    (if (= :once-breakpoint kind)
      (remove-breakpoint cpu (pc cpu))
      (set-breakpoint cpu (pc cpu) kind))))

(defn stop-debugging [cpu]
  (assoc cpu :break? nil))

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

(defn decode-from
  ([cpu] (decode-from cpu (pc cpu)))
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
  [(fn [cpu]
     (let [instruction (instruction-at-pc cpu)]
       (set-breakpoint cpu (pc (exec cpu instruction)) :once-breakpoint)))
   (partial ->debug-view {})])

(defmethod handle-debug-command :back-step
  [_]
  [history/restore! (partial ->debug-view {})])

(defmethod handle-debug-command :step-over
  [_]
  [(fn [cpu]
     (let [{[kind & _] :asm size :size :as instruction} (instruction-at-pc cpu)]
       (if (= :call kind)
         (set-breakpoint cpu (%16 + (pc cpu) size) :once-breakpoint)
         (set-breakpoint cpu (pc (exec cpu instruction)) :once-breakpoint))))
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

(defn process-breakpoint [{:keys [debug-chan-tx debug-chan-rx] :as cpu}]
  (>!! debug-chan-tx {:command :break})
  (-> (process-debug-command cpu (<!! debug-chan-rx))
      (after-break)
      (assoc :break? nil)))

