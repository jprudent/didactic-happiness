(ns repicene.assembler
  "This namespace provides an assembler"
  (:require [clojure.spec :as spec]
            [repicene.schema :as s]
            [repicene.decoder :as decoder]
            [clojure.string :as str]))

(defn last-arg-dword [s]
  (-> (re-matches #".* (\d{1,4})" s)
      (last)
      (Long/parseLong)
      ((juxt decoder/low-word decoder/high-word))))

(defn instruction-lookup
  [assembly]
  {:pre  [(string? assembly)]
   :post [(spec/valid? (spec/coll-of ::s/word) %)]}
  (cond
    (= "nop" assembly)
    [0x00]

    (str/starts-with? assembly "ld bc ")
    (concat [0x01] (last-arg-dword assembly))

    (= "ld <bc> a" assembly)
    [0x02]

    (= "inc bc" assembly)
    [0x03]

    (= "halt" assembly)
    [0x76]

    (= "ret" assembly)
    [0xC9]

    (str/starts-with? assembly "call ")
    (concat [0xCD] (last-arg-dword assembly))))

(defn assemble
  [program]
  {:pre  [(string? program)]
   :post [(spec/valid? (spec/coll-of ::s/word) %)]}
  (->> (str/split-lines program)
       (mapcat instruction-lookup)))