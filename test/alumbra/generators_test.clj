(ns alumbra.generators-test
  (:require [alumbra
             [parser :as parser]
             [analyzer :as analyzer]
             [generators :as gens]]
            [clojure.test.check
             [generators :as gen]
             [properties :as prop]
             [clojure-test :refer [defspec]]]))

(def schema
  (-> "type Person { name: String!, pets: [Pet!] }
       type Pet { name: String!, meows: Boolean }
       union PersonOrPet = Person | Pet
       enum PositionKind { LONG, LAT }
       input Position { x: Int, y: Int, k: PositionKind! }
       directive @live (timeout: Int!) on FIELD, INLINE_FRAGMENT
       type QueryRoot { person(name: String!): Person, random(seed: Position!): PersonOrPet }
       type MutationRoot { createPerson(name: String!): Person! }
       schema { query: QueryRoot, mutation: MutationRoot }"
      (analyzer/analyze-schema parser/parse-schema)))

(defspec t-valid-operation 1000
  (let [gen-operation (gens/operation schema)]
    (prop/for-all
      [operation (gen/one-of
                   [(gen-operation :query)
                    (gen-operation :mutation)])]
      (not
        (:alumbra/parser-errors
          (parser/parse-document operation))))))
