(ns repicene.debug-test
  (:require [clojure.test :refer :all]
            [repicene.core :refer :all]
            [repicene.debug :refer :all]
            [repicene.decoder :refer :all]
            [repicene.assembler :as asm]
            [repicene.schema :as s]
            [clojure.core.async :as async]
            [repicene.file-loader :refer [load-rom]]
            [clojure.string :as str]))



(deftest test-decode
  (testing "decode"
    (is (= 0x100)
        (->> (decode-from (-> (take 0x8000 (load-rom "roms/cpu_instrs/cpu_instrs.gb"))
                              (new-cpu)
                              (pc 0x100))
                          0)
             (take 0x100)
             (count)))))

(defn run-gameboy
  ([bp prog]
   (let [{:keys [debug-chan-rx debug-chan-tx] :as cpu}
         (-> (take 0x8000 (cycle (asm/assemble prog)))
             (new-cpu)
             (pc bp)
             (set-breakpoint bp :permanent-breakpoint))
         looping-cpu (async/thread (cpu-loop cpu))]
     [looping-cpu debug-chan-rx debug-chan-tx]))
  ([bp] (run-gameboy bp "inc bc")))

(deftest test-debugging

  (testing "client must acknowledge break before sending commands"
    (let [[looping-cpu tx rx] (run-gameboy 0)
          inspect-cmd ::s/inspect]
      ;; initial state is break
      (is (= {:command :break} (async/<!! rx)) "the cpu should break")

      ;; try to send a command
      (is (async/>!! tx inspect-cmd)
          "the cpu should receive command because it breaks")
      (is (= {:command :break} (async/<!! rx))
          "the cpu should is waiting for acknowledge")

      ;; acknowledge the break
      (is (async/>!! tx ::s/ack-break))

      ;; now cpu should respond to command
      (async/>!! tx inspect-cmd)
      (is (= inspect-cmd (:command (async/<!! rx))) "the cpu should respond to command")

      ;; kill the gameboy
      (is (async/>!! tx ::s/kill))
      (is (nil? (async/<!! looping-cpu)))))

  (testing "step over"
    (let [[looping-cpu tx rx] (run-gameboy 0)]
      ;; initialize the debugging session
      (is (= {:command :break} (async/<!! rx)) "the cpu should break")
      (is (async/>!! tx ::s/ack-break))

      ;; now cpu should respond to command
      (async/>!! tx ::s/step-over)
      (is (= ::s/step-over (:command (async/<!! rx))))

      (async/>!! tx ::s/inspect)
      (is (= 1 (get-in (async/<!! rx) [:response ::s/registers ::s/PC])))

      (async/>!! tx ::s/step-over)
      (is (= ::s/step-over (:command (async/<!! rx))))

      (async/>!! tx ::s/inspect)
      (let [cpu (:response (async/<!! rx))]
        (prn cpu)
        (is (= 2 (get-in cpu [::s/registers ::s/PC])))
        (is (= [3 :permanent-breakpoint] (get-in cpu [::s/x-breakpoints 0]))
            "permanent breakpoint is not lost"))

      ;; kill the gameboy
      (is (async/>!! tx ::s/kill))
      (is (nil? (async/<!! looping-cpu)))))

  (testing "resume with step over"
    (let [[looping-cpu tx rx] (run-gameboy 0)]
      ;; initialize the debugging session
      (is (= {:command :break} (async/<!! rx)) "the cpu should break")
      (is (async/>!! tx ::s/ack-break))

      ;; now cpu should respond to command
      (async/>!! tx ::s/step-over)
      (is (= 1 (get-in (async/<!! rx) [:response ::s/registers ::s/PC])))

      ;; resume
      (async/>!! tx ::s/resume)
      (is (= ::s/resume (:command (async/<!! rx))) "resume command is received")

      ;; let the gameboy run for a while
      (Thread/sleep 1000)

      ;;inspect
      (async/>!! tx ::s/inspect)
      (is (> (get-in (async/<!! rx) [:response ::s/registers ::s/PC]) 1))

      ;; kill the gameboy
      (is (async/>!! tx ::s/kill))
      (is (nil? (async/<!! looping-cpu)))))

  (testing "resume without step over"
    (let [[looping-cpu tx rx] (run-gameboy 0)]
      ;; initialize the debugging session
      (is (= {:command :break} (async/<!! rx)) "the cpu should break")
      (is (async/>!! tx ::s/ack-break))

      ;; resume
      (async/>!! tx ::s/resume)
      (is (= ::s/resume (:command (async/<!! rx))) "resume command is received")

      ;; let the gameboy run for a while
      (Thread/sleep 1000)

      ;;inspect
      (async/>!! tx ::s/inspect)
      (is (> (get-in (async/<!! rx) [:response ::s/registers ::s/PC]) 1))

      ;; kill the gameboy
      (is (async/>!! tx ::s/kill))
      (is (nil? (async/<!! looping-cpu)))))

  (testing "step over call"
    (let [pg [#_0 "call 4"
              #_3 "inc bc"
              #_4 "ret"]
          [looping-cpu tx rx] (run-gameboy 0 (str/join "\n" pg))]
      ;; initialize the debugging session
      (is (= {:command :break} (async/<!! rx)) "the cpu should break")
      (is (async/>!! tx ::s/ack-break))

      (async/>!! tx ::s/step-over)
      (is (= 3 (get-in (async/<!! rx) [:response ::s/registers ::s/PC])))

      ;; kill the gameboy
      (is (async/>!! tx ::s/kill))
      (is (nil? (async/<!! looping-cpu)))))

  (testing "step into call"
    (let [pg [#_0 "call 4"
              #_3 "inc bc"
              #_4 "ret"]
          [looping-cpu tx rx] (run-gameboy 0 (str/join "\n" pg))]
      ;; initialize the debugging session
      (is (= {:command :break} (async/<!! rx)) "the cpu should break")
      (is (async/>!! tx ::s/ack-break))

      (async/>!! tx ::s/step-into)
      (is (= 4 (get-in (async/<!! rx) [:response ::s/registers ::s/PC])))

      ;; kill the gameboy
      (is (async/>!! tx ::s/kill))
      (is (nil? (async/<!! looping-cpu)))))

  (testing "return"
    (let [pg [#_0 "call 4"
              #_3 "inc bc"
              #_4 "inc bc"
              #_5 "inc bc"
              #_6 "ret"]
          [looping-cpu tx rx] (run-gameboy 0 (str/join "\n" pg))]
      ;; initialize the debugging session
      (is (= {:command :break} (async/<!! rx)) "the cpu should break")
      (is (async/>!! tx ::s/ack-break))

      (async/>!! tx ::s/step-into)
      (is (= 4 (get-in (async/<!! rx) [:response ::s/registers ::s/PC])))

      (async/>!! tx ::s/return)
      (let [cpu (:response (async/<!! rx))]
        (is (= 3 (get-in cpu [::s/registers ::s/PC])))
        (is (= 2 (get-in cpu [::s/registers ::s/BC]))))

      ;; kill the gameboy
      (is (async/>!! tx ::s/kill))
      (is (nil? (async/<!! looping-cpu))))))
