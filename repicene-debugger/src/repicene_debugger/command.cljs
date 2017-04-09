(ns repicene-debugger.command
  (:require [cljs.core.async :refer [>!]]
            [repicene-debugger.communication :refer [tx]]
            [repicene.schema :as s])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn do-resume []
  (go (>! tx :resume)))

(defn do-step-into []
  (go (>! tx :step-into)))

(defn do-step-over []
  (go (>! tx :step-over)))

(defn do-back-step []
  (go (>! tx :back-step)))

(defn do-return []
  (go (>! tx :return)))

(defn do-reset []
  (go (>! tx :reset)))

(defn add-breakpoint [address]
  {:pre [(s/address? address)]}
  (go (>! tx [:add-breakpoint address])))

(defn remove-breakpoint [address]
  (go (>! tx [:remove-breakpoint address])))

(defn inspect-params []
  [:inspect {:regions [[0xDF00 0xDFFF]]}])

(defn do-inspect []
  (go (>! tx (inspect-params))))
