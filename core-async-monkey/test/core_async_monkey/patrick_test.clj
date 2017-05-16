(ns core-async-monkey.patrick-test
  (:require [clojure.test :refer [with-test is]]))

(defprotocol Invoice
  (due-for? [_ month])
  (including-VAT [this])
  (total-including-VAT [this]))

(defrecord DetailedInvoice [due-month excluded-vat vat-rate quantity paid?]
  Invoice
  (due-for? [_ month] (and (not paid?)
                           (= due-month month)))
  (including-VAT [_]
    (+ excluded-vat
       (* (/ vat-rate 100) excluded-vat)))
  (total-including-VAT [this]
    (* (including-VAT this) quantity)))



(def invoices [(->DetailedInvoice 'October 36 20 1 false)
               (->DetailedInvoice 'September 28 10 1 true)
               (->DetailedInvoice 'October 17 5 2 false)
               (->DetailedInvoice 'October 27 5 2 true)])

(def expected-for-october
  (+ (+ 36 (* 36 20/100)) (* 2 (+ 17 (* 17 5/100)))))

(with-test
  (defn get-amount-of-2
    "This is an exact copy of the one of the blog post"
    [invoices month]
    (let [invoices-of-the-month (filter (fn [invoice] (due-for? invoice month)) invoices)
          include-vat-amount    (map (fn [invoice] (total-including-VAT invoice)) invoices-of-the-month)]
      (reduce + 0 include-vat-amount)))
  (is (= expected-for-october (get-amount-of-2 invoices 'October))))

(with-test
  (defn get-amount-of-3
    "Same as get-amount-of-2 but with macros and syntactic sugar"
    [invoices month]
    (->> (filter #(due-for? % month) invoices)
         (map total-including-VAT)
         (reduce + 0)))
  (is (= expected-for-october (get-amount-of-3 invoices 'October))))

(defn reduce-from-scratch [reducer id coll]
  (loop [acc id
         [x & others] coll]
    (if (some? x)
      (recur (reducer acc x) others)
      acc)))

(defn map-from-scratch [mapper coll]
  (loop [acc (empty coll)
         [x & others] coll]
    (if (some? x)
      (recur (conj acc (mapper x)) others)
      acc)))

(defn filter-from-scratch [pred coll]
  (loop [acc (empty coll)
         [x & others] coll]
    (if (some? x)
      (recur (if (pred x) (conj acc x) acc) others)
      acc)))

(with-test
  (defn acc-letter [acc letter]
    (update acc letter #(inc (or % 0))))
  (is (= {\space 6 \a 1 \c 2 \d 1 \e 4 \f 2 \I 1 \i 3 \é 1 \l 1 \n 4 \o 3 \r 1 \s 1 \t 3 \u 3}
         (reduce-from-scratch acc-letter {} "Il était une fois une fonction reduce"))))

(defn map-with-reduce [mapper coll]
  (letfn [(map-reducer [acc x] (conj acc (mapper x)))]
    (reduce-from-scratch map-reducer
                         (empty coll)
                         coll)))

(defn filter-with-reduce [pred coll]
  (letfn [(filter-reducer [acc x] (if (pred x) (conj acc x) acc))]
    (reduce-from-scratch filter-reducer
                         (empty coll)
                         coll)))

(with-test
  (defn get-amount-of-4
    "Same as get-amount-of-3 but with our own map filter implementation"
    [invoices month]
    (->> (filter-with-reduce #(due-for? % month) invoices)
         (map-with-reduce total-including-VAT)
         (reduce-from-scratch + 0)))
  (is (= expected-for-october (get-amount-of-4 invoices 'October))))

(defn map-transducer [mapper]
  (fn [acc x] (conj acc (mapper x))))

(defn filter-transducer [pred]
  (fn [acc x] (if (pred x) (conj acc x) acc)))

(with-test
  (defn get-amount-of-5
    "Using transducer functions"
    [invoices month]
    (->> (reduce (filter-transducer #(due-for? % month)) (empty invoices) invoices)
         (reduce (map-transducer total-including-VAT) [])
         (reduce-from-scratch + 0)))
  (is (= expected-for-october (get-amount-of-5 invoices 'October))))

;; ouch

(defn filter-transducer-1 [pred next-reducer]
  (fn [acc x]
    (if (pred x) (next-reducer acc x) acc)))

(defn map-transducer-1 [mapper next-reducer]
  (fn [acc x]
    (next-reducer acc (mapper x))))

(defn and-then [transducer-1 transducer-2]
  (transducer-1 transducer-2))

(with-test
  (defn get-amount-of-6
    "Composing transducers"
    [invoices month]
    (reduce (comp
              +
              (filter-transducer #(due-for? % month))
              (map-transducer total-including-VAT))
            (empty invoices)
            invoices))
  (is (= expected-for-october (get-amount-of-6 invoices 'October))))