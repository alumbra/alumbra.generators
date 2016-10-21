(ns alumbra.generators.document
  (:require [clojure.test.check.generators :as gen]
            [alumbra.generators
             [fragments :refer [-fragment-definition]]
             [operations :refer [-operation-definition]]]
            [clojure.string :as string]))

(def -definition
  "Generate a single GraphQL definition, being one of:

   - operation
   - fragment
   "
  (gen/one-of
    [-operation-definition
     -fragment-definition]))

(def -document
  "Generate a GraphQL document, consisting of operations and fragments."
  (->> (gen/vector -definition 1 5)
       (gen/fmap #(string/join "\n" %))))
