(ns repicene.schema
  (:require [clojure.spec :as s]
            [clojure.spec :as s]
            [clojure.spec :as s]
            [clojure.spec :as s]
            [clojure.spec :as s]))

(s/def ::dword (s/and integer? #(<= 0 % 0xFFFF)))
(s/def ::address ::dword)
(s/def ::word (s/and integer? #(<= 0 % 0xFF)))
(s/def ::AF ::dword)
(s/def ::BC ::dword)
(s/def ::DE ::dword)
(s/def ::HL ::dword)
(s/def ::SP ::dword)
(s/def ::PC ::dword)
(s/def ::registers (s/keys :req [::AF ::BC ::DE ::HL ::SP ::PC]))
(s/def ::interrupt-enabled? boolean?)
(s/def ::memory-backend
  (s/and
    (s/tuple ::address ::address (s/and vector? (s/coll-of ::word)))
    (fn [[start end _]] (< start end))
    (fn [[start end mem]] (= (count mem) (inc (- end start))))))
(s/def ::memory (s/and vector? (s/coll-of ::memory-backend :kind vector?)))

(s/def ::mode #{::running ::stopped ::halted})
(s/def ::x-breakpoint (s/tuple ::address #{:once-breakpoint :permanent-breakpoint}))
(s/def ::x-breakpoints (s/map-of ::address ::x-breakpoint))
(s/def ::cpu (s/keys :req [::registers
                           ::interrupt-enabled?
                           ::memory
                           ::mode
                           ::x-breakpoints]))

(s/def ::nibble (s/and integer? #(<= 0 % 0xF)))

(defn validate [kw]
  #_(constantly true)
  (partial s/valid? kw))

(def valid? (validate ::cpu))
(def dword? (validate ::dword))
(def address? (validate ::address))
(def word? (validate ::word))
(def memory? (validate ::memory))
(def nibble? (validate ::nibble))
(def x-breakpoint? (validate ::x-breakpoint))

(s/def ::disassembled (s/tuple ::address (s/and (s/coll-of ::word) #(<= 1 (count %) 3)) string?))