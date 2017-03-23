(ns repicene-debugger.ui
  "Contains all the components that makes the ui"
  (:require [clojure.string :refer [split replace]]
            [goog.string :as gstring]
            [goog.string.format]
            [clojure.string :as string]))

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

(defn bem
  ([block element] (bem block element nil))
  ([block element modifiers]
   (string/join
     " "
     (let [modifiers (filter (comp not nil?) modifiers)]
       (if (nil? element)
         (cons block (map #(str block "__" %) modifiers))
         (cons (str block "-" element) (map #(str block "-" element "__" %) modifiers)))))))

(defn register [register value]
  (let [bem (partial bem "debugger")]
    ^{:key register}
    [:div
     {:class (bem "register")}
     [:span {:class (bem "registerName")} (name register)]
     [:span {:class (bem "registerValue")} (hex-dword value)]]))

(defn window-title [title]
  [:h1.debugger-windowTitle title])

(def empty-button [:div.debugger-button])

(defn registers
  "returns the UI component that display the registers"
  [{:keys [registers]}]
  (when registers
    [:div.debugger-registers
     (window-title "Registers")
     (map (fn [register-name]
            (register register-name (register-name registers)))
          [:AF :BC :DE :HL :SP :PC])]))

(defn instruction
  [pc [address bytes asm :as key]]
  "foo"
  (let [block     "debugger"
        bem       (partial bem block)
        modifiers (partial bem "instructionLine")]
    ^{:key key} [:div
                 {:class (modifiers [(when (= pc address) "atPc")])}
                 [:div {:class (bem "address")} (hex-dword address)]
                 [:div {:class (bem "hexabytes")} (apply str (map hex-word bytes))]
                 [:div {:class (bem "asm")} asm]]))

(defn instructions
  [{:keys [instructions]} pc]
  (when instructions
    [:div.debugger-instructions
     (window-title "Program")
     (map (partial instruction pc) instructions)]))
