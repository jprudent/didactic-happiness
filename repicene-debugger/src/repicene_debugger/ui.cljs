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

(defn- hex
  [x width]
  (-> (.toString x 16)
      (format (str "%0" width "s"))
      (replace " " "0")))

(defn hex-dword
  "returns a dword hexadecimal formatted string representation of x"
  [x]
  {:pre [(<= 0 x 0xFFFF)]}
  (hex x 4))

(defn hex-word
  "returns a word hexadecimal formatted string representation of x"
  [x]
  {:pre [(<= 0 x 0xFF)]}
  (hex x 2))

(defn register [register value]
  ^{:key register} [:div [:span (name register)] [:span (hex-dword value)]])

(defn registers
  "returns the UI component that display the registers"
  [{:keys [registers]}]
  (when registers
    [:div
     (map (fn [register-name]
            (register register-name (register-name registers)))
          [:AF :BC :DE :HL :SP :PC])]))

(defn instruction
  [[address bytes asm :as key]]
  ^{:key key} [:div
               [:span (hex-dword address)]
               [:span (apply str (map hex-word bytes))]
               [:span asm]])

(defn instructions
  [instructions]
  (when instructions
    [:div (map instruction instructions)]))
