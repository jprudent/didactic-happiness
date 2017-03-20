(ns repicene-debugger.ui
  "Contains all the components that makes the ui"
  (:require [clojure.string :refer [split replace]]
            [goog.string :as gstring]
            [goog.string.format]))

(defn format
  "wrapper for gstring/format that unfortunately misplaced arguments,
  hence not compatible with threading macro"
  [s pattern]
  (gstring/format pattern s))

(defn hex
  "returns a dword hexadecimal formatted string representation of x"
  [x]
  {:pre [(<= 0 x 0xFFFF)]}
  (-> (.toString x 16)
      (format "%04s")
      (replace " " "0")))

(defn register [register value]
  ^{:key register} [:div [:span (name register)] [:span (hex value)]])

(defn registers
  "returns the UI component that display the registers"
  [{:keys [registers]}]
  (when registers
    [:div
     (map (fn [register-name]
             (register register-name (register-name registers)))
          [:AF :BC :DE :HL :SP :PC])]))
