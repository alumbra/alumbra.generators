(ns alumbra.generators
  (:require [alumbra.generators
             [selection-set :refer [selection-set-generators]]]
            [clojure.test.check
             [generators :as gen]]
            [clojure.string :as string]))

;; ## Operation Types

(defn query
  [schema]
)

(defn mutation
  [schema]
  )

(defn subscription
  [schema]
  )

;; ## Operation

(defn operation
  "Generate a GraphQL operation."
  [schema]
  (if-let [gens (->> [(query schema)
                      (mutation schema)
                      (subscription schema)]
                     (filter some?)
                     (seq))]
    (gen/one-of gens)
    (gen/return nil)))

;; ## Document

(defn document
  "Generate a GraphQL document, containing at least one operation, matching the
   given schema."
  [schema]
  (->> (gen/vector (operation schema) 1 3)
       (gen/fmap #(filter some? %))
       (gen/fmap #(string/join "\n" %))))

;; ## Example

(comment
  (require '[alumbra [parser :as parser] [analyzer :as analyzer]])

  (def ss
    (selection-set-generators
      {:schema
       (-> "type Person { name: String!, pets: [Pet!] }
            type Pet { name: String!, meows: Boolean }
            union PersonOrPet = Person | Pet
            type QueryRoot { person(name: String!): Person, random: PersonOrPet }
            schema { query: QueryRoot }"
           (analyzer/analyze-schema parser/parse-schema))
       :value-gen (constantly (gen/return "YYY"))
       }))

  (last (gen/sample (ss "QueryRoot") 100)))
