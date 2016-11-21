(ns alumbra.generators
  (:require [alumbra.generators
             [selection-set :refer [selection-set-generators]]
             [value :refer [value-generators]]]
            [clojure.test.check
             [generators :as gen]]
            [clojure.string :as string]))

;; ## TODO
;;
;; - Variables
;; - Named Fragments

;; ## Helpers

(defn- make-operation-gen
  [{:keys [schema selection-set-gen]} k]
  (when-let [t (get-in schema [:schema-root :schema-root-types (name k)])]
    (selection-set-gen t)))

;; ## Public Functions

(defn operation
  "Create a function that can be called with an operation type (e.g.
   :query, :mutation, :subscription), as well as the name of the operation,
   and will produce an operation matching the given analyzed schema.

   ```clojure
   (def schema
     (alumbra.analyzer/analyze-schema
       \"type Person { ... } ... \"
       alumbra.parser/parse-schema))

   (def gen-operation
     (operation schema))

   (gen/sample (gen-operation :query \"Q\"))
   ```

   `schema` must conform to `:alumbra/analyzed-schema` (see alumbra.spec)."
  [schema & [opts]]
  (let [opts (assoc opts :schema schema)
        value-gen (value-generators opts)
        opts (assoc opts :value-gen value-gen)
        selection-set-gen (selection-set-generators opts)
        opts (assoc opts :selection-set-gen selection-set-gen)
        type->gen {:query        (make-operation-gen opts :query)
                   :mutation     (make-operation-gen opts :mutation)
                   :subscription (make-operation-gen opts :subscription)}]
    (fn [k operation-name]
      (if-let [gen (type->gen k)]
        (gen/fmap
          #(str (name k) " " (name operation-name) " " %)
          gen)
        (throw
          (IllegalArgumentException.
            (str "no generator for operation type: " k)))))))

;; ## Example

(comment
  (require '[alumbra [parser :as parser] [analyzer :as analyzer]])
  (def schema
    (-> "type Person { name: String!, pets: [Pet!] }
         type Pet { name: String!, meows: Boolean }
         union PersonOrPet = Person | Pet
         enum PositionKind { LONG, LAT }
         input Position { x: Int, y: Int, k: PositionKind! }
         type QueryRoot { person(name: String!): Person, random(seed: Position!): PersonOrPet }
         type MutationRoot { createPerson(name: String!): Person! }
         schema { query: QueryRoot, mutation: MutationRoot }"
        (analyzer/analyze-schema parser/parse-schema)))

  (def gen (operation schema))

  (last (gen/sample (gen :query "Q") 100)))
