(ns repicene.debug-test
  (:require [clojure.test :refer :all]
            [repicene.core :refer :all]
            [repicene.debug :refer :all]
            [repicene.decoder :refer :all]
            [repicene.schema :as s]
            [repicene.file-loader :refer [load-rom]]))



#_(deftest test-decode
  (testing "decode"
    (doseq [instr (take 0x8000 (decode-from (-> (load-rom "roms/cpu_instrs/cpu_instrs.gb")
                                           (new-cpu)
                                           (pc 0x100)) 0xFFFE))]
      (println instr)))
  )
