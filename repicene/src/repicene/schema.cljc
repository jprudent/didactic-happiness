(ns repicene.schema
  (:require [clojure.spec :as s]))

(declare valid?)
(defn validate [spec]
  (fn [v]
    (if (s/valid? spec v)
      v
      (do (s/explain spec v) false))))
(def word? (validate ::word))
(s/def ::dword (s/int-in 0 0x10000))
(s/def ::address ::dword)
(s/def ::word (s/int-in 0 0x100))
(s/def ::AF ::dword)
(s/def ::BC ::dword)
(s/def ::DE ::dword)
(s/def ::HL ::dword)
(s/def ::SP ::dword)
(s/def ::PC ::dword)
(s/def ::registers (s/keys :req-un [::AF ::BC ::DE ::HL ::SP ::PC]))
(s/def ::interrupt-enabled? boolean?)
(s/def ::memory (partial every? word?))
(s/def ::mode #{::running ::stopped ::halted ::break ::debugging})
(s/def ::x-breakpoint (s/tuple ::address #{:once-breakpoint :permanent-breakpoint}))
(s/def ::x-breakpoints (s/map-of ::address ::x-breakpoint))
(s/def ::cpu (s/keys :req-un [::registers
                              ::interrupt-enabled?
                              ::memory
                              ::mode
                              ::x-breakpoints]))

(s/def ::nibble (s/and integer? #(<= 0 % 0xF)))

(def cpu? (validate ::cpu))
(def dword? (validate ::dword))
(def address? (validate ::address))

(def memory? (validate ::memory))
(def nibble? (validate ::nibble))
(def x-breakpoint? (validate ::x-breakpoint))
(def command? (validate ::command))

(s/def ::disassembled (s/tuple ::address (s/and (s/coll-of ::word :min-count 1 :max-count 3)) string?))

(s/def ::command-type #{::inspect ::ack-break ::kill ::step-over ::step-into ::return ::resume})
(s/def ::command-arg some?)
(s/def ::command (s/or :simple ::command-type
                       :parametrized (s/cat :type ::command-type :arg ::command-arg)))