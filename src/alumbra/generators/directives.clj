(ns alumbra.generators.directives
  (:require [clojure.test.check.generators :as gen]
            [clojure.string :as string]
            [alumbra.generators
             [arguments :refer [-arguments]]
             [common :refer [-name rarely]]]))

(def -directive
  "Generate a single GraphQL directive."
  (gen/let [n -name
            a (rarely -arguments)]
    (gen/return (str "@" n a))))

(def -directives
  "Generate multiple GraphQL directives, separated by a space."
  (->> (gen/vector -directive 1 3)
       (gen/fmap #(string/join " " %))))
