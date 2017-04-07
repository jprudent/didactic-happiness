(ns repicene.schema
  (:require [clojure.spec :as s]
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
(s/def ::memory-backend (s/tuple ::address ::address coll?))
(s/def ::memory (and vector? (s/coll-of ::memory-backend :kind vector?)))
(s/def ::history (and list? #(<= (count %) 100)))
(s/def ::mode #{::running ::stopped})
(s/def ::x-breakpoints (and set? (s/coll-of ::address)))
(s/def ::cpu (s/keys :req [::registers
                           ::interrupt-enabled?
                           ::memory
                           ::history
                           ::mode
                           ::x-breakpoints]))

(s/def ::nibble (s/and integer? #(<= 0 % 0xF)))

(def valid? (partial s/valid? ::cpu))
(def dword? (partial s/valid? ::dword))
(def address? (partial s/valid? ::address))
(def word? (partial s/valid? ::word))
(def memory? (partial s/valid? ::memory))
(def nibble? (partial s/valid? ::nibble))