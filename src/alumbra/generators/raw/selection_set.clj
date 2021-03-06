(ns alumbra.generators.raw.selection-set
  (:require [clojure.test.check.generators :as gen]
            [alumbra.generators.raw
             [arguments :refer [-arguments]]
             [common :refer :all]
             [directives :refer [-directives]]
             [type :refer [-type]]]
            [clojure.string :as string]))

(declare -selection-set)

(def ^:no-doc -type-condition
  (gen/fmap #(str "on " (string/capitalize %)) -name))

(def ^:no-doc -fragment-spread
  (gen/let [n (gen/such-that #(not= % "on") -name)
            d (rarely -directives)]
    (gen/return (str "..." n (some->> d (str " "))))))

(def ^:no-doc -inline-fragment
  (gen/let [t (maybe -type-condition)
            d (rarely -directives)
            s -selection-set]
    (gen/return
      (str "..."
           (some->> t (str " "))
           (some->> d (str " "))
           " " s))))

(def ^:no-doc -field
  (gen/let [a (rarely -name)
            n -name
            g (maybe -arguments)
            d (rarely -directives)
            s (rarely -selection-set)]
    (gen/return
      (str (some-> a (str ": "))
           (if g (str n g) n)
           (some->> d (str " "))
           (some->> s (str " "))))))

(def ^:no-doc -selection
  (gen/frequency
    [[90 -field]
     [5 -fragment-spread]
     [5 -inline-fragment]]))

(def -selection-set
  "Generate a valid GraphQL selection set."
  (gen/let [selections (gen/vector -selection 1 5)]
    (gen/return
      (format "{%s}" (string/join ", " selections)))))
