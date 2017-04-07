(ns repicene-debugger.ui
  "Contains all the components that makes the ui"
  (:require [clojure.string :refer [split replace]]
            [goog.string :as gstring]
            [goog.string.format]
            [clojure.string :as string]
            [repicene.schema :as s]
            [repicene-debugger.command :as cmd]))

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
  [{{:keys [::s/AF] :as registers} ::s/registers}]
  (when registers
    [:div.debugger-registers
     (window-title "Registers")
     (map (fn [register-name]
            (register register-name (register-name registers)))
          [::s/AF
           ::s/BC
           ::s/DE
           ::s/HL
           ::s/SP
           ::s/PC])
     [:div.debugger-register
      [:span.debugger-registerName "Flgs"]
      [:span
       (if (bit-test AF 7) "Z" "z")
       (if (bit-test AF 6) "N" "n")
       (if (bit-test AF 5) "H" "h")
       (if (bit-test AF 4) "C" "c")]]]))

(defn instruction
  [pc x-breakpoints [address bytes asm :as key]]
  "foo"
  (let [block          "debugger"
        debugger-block (partial bem block)
        line-elem      (partial debugger-block "instructionLine")
        has-bp         (x-breakpoints address)
        toggle-bp      (if has-bp
                         (partial cmd/remove-breakpoint address)
                         (partial cmd/add-breakpoint address))]
    ^{:key key} [:div
                 {:class (line-elem [(when (= pc address) "atPc")])}
                 [:div {:class    (debugger-block "bp")
                        :on-click toggle-bp}
                  (when has-bp "●")]
                 [:div {:class (debugger-block "address")} (hex-dword address)]
                 [:div {:class (debugger-block "hexabytes")} (apply str (map hex-word bytes))]
                 [:div {:class (debugger-block "asm")} asm]]))

(defn instructions
  [{:keys [instructions ::s/x-breakpoints]} pc]
  (when instructions
    [:div.debugger-instructions
     (window-title "Program")
     (map (partial instruction pc x-breakpoints) instructions)]))

(defn address-dump
  [[address content]]
  (let [block          "debugger"
        debugger-block (partial bem block)]
    ^{:key address} [:div {:class (debugger-block "memoryLine")}
                     [:div {:class (debugger-block "address")} (hex-dword address)]
                     [:div {:class (debugger-block "hexabytes")} (hex-dword content)]]))

(defn memory
  [{[[start end dump :as sp-region] & _] :regions}]
  (when sp-region
    [:div.debugger-memoryDump
     (window-title (str "Dump [" (hex-dword start) "-" (hex-dword end) "]"))
     [:div.debugger-memoryDumpContent (map address-dump dump)]]))
