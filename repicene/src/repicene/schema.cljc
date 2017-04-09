(ns repicene.schema
  (:require [clojure.spec :as s]
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
  (s/tuple ::address ::address (s/and vector? (s/coll-of ::word)))
  #_(s/and
      (fn [[start end _]] (< start end))
      (fn [[start end mem]] (= (count mem) (inc (- end start))))))
(s/def ::memory (s/and vector? (s/coll-of ::memory-backend :kind vector?)))
(s/def ::history (s/or :no-history nil?
                       :with-history (s/and coll? #(<= (count %) 100))))
(s/def ::mode #{::running ::stopped})
(s/def ::x-breakpoints (and set? (s/coll-of ::address)))
(s/def ::cpu (s/keys :req [::registers
                           ::interrupt-enabled?
                           ::memory
                           ::history
                           ::mode
                           ::x-breakpoints]))

(s/def ::nibble (s/and integer? #(<= 0 % 0xF)))

(defn validate [kw]
  (constantly true)
  #_(partial s/valid? kw))
(def valid? (validate ::cpu))
(def dword? (validate ::dword))
(def address? (validate ::address))
(def word? (validate ::word))
(def memory? (validate ::memory))
(def nibble? (validate ::nibble))

(s/def ::disassembled (s/tuple ::address (s/and (s/coll-of ::word) #(<= 1 (count %) 3)) string?))