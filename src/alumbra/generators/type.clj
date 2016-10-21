(ns alumbra.generators.type
  (:require [clojure.test.check.generators :as gen]
            [alumbra.generators.common :refer [-name]]
            [clojure.string :as string]))

(def -type-name
  "Generate a valid GraphQL type name."
  (gen/fmap string/capitalize -name))

(def -type
  "Generate a valid GraphQL type, including list and non-nullable ones."
  (gen/let [n     -type-name
            list? gen/boolean
            req?  gen/boolean]
    (gen/return
      (cond->> n
        list? (format "[%s]")
        req?  (format "%s!")))))
