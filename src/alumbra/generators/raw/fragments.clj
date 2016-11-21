(ns alumbra.generators.raw.fragments
  (:require [clojure.test.check.generators :as gen]
            [alumbra.generators.raw
             [common :refer [-name rarely]]
             [directives :refer [-directives]]
             [selection-set :refer [-selection-set -type-condition]]]
            [clojure.string :as string]))

(def -fragment-name
  "Generate a valid name for a GraphQL fragment."
  (gen/fmap string/capitalize (gen/such-that #(not= % "on") -name)))

(def -fragment-definition
  "Generate a valid GraphQL fragment definition."
  (gen/let [n -fragment-name
            t -type-condition
            d (rarely -directives)
            s -selection-set]
    (str "fragment "
         n " "
         t " "
         (some-> d (str " "))
         s)))
