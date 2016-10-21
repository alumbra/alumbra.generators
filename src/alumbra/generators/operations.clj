(ns alumbra.generators.operations
  (:require [clojure.test.check.generators :as gen]
            [alumbra.generators
             [common :refer [-name maybe rarely]]
             [directives :refer [-directives]]
             [selection-set :refer [-selection-set]]
             [variables :refer [-variable-definitions]]]
            [clojure.string :as string]))

(defn- named-operation-gen
  [t]
  (gen/let [n (gen/fmap string/capitalize -name)
            v (maybe -variable-definitions)
            d (rarely -directives)
            s -selection-set]
    (gen/return
      (str t " " n v
           (some->> d (str " "))
           " " s))))

(def -query-definition
  "Generate a GraphQL `query` definition."
  (named-operation-gen "query"))

(def -mutation-definition
  "Generate a GraphQL `mutation` definition."
  (named-operation-gen "mutation"))

(def -subscription-definition
  "Generate a GraphQL `subscription` definition."
  (named-operation-gen "subscription"))

(def -operation-definition
  "Generate a GraphQL operation definition (e.g. `query`)."
  (gen/one-of
    [-selection-set
     -query-definition
     -mutation-definition
     -subscription-definition]))
