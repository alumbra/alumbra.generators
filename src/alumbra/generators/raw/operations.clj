(ns alumbra.generators.raw.operations
  (:require [clojure.test.check.generators :as gen]
            [alumbra.generators.raw
             [common :refer [-name maybe rarely]]
             [directives :refer [-directives]]
             [selection-set :refer [-selection-set]]
             [variables :refer [-variable-definitions]]]
            [clojure.string :as string]))

(defn- named-operation-gen
  [t]
  (gen/let [t (if (= t "query") (maybe (gen/return t)) (gen/return t))
            n  (maybe (gen/fmap string/capitalize -name))
            v  (maybe -variable-definitions)
            d  (rarely -directives)
            s  -selection-set]
    (gen/return
      (if t
        (str t " "
             (some-> n (str " "))
             (some-> v (str " "))
             (some-> d (str " "))
             s)
        s))))

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
