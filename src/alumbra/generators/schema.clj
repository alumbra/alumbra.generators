(ns alumbra.generators.schema
  (:require [clojure.test.check.generators :as gen]
            [alumbra.generators
             [common :refer [-name maybe rarely]]
             [type :refer [-type -type-name]]
             [value :refer [-float -string -bool -enum -integer]]]
            [clojure.string :as string]))

;; ## Schema Directives

(def -argument
  (gen/let [n -name
            v (gen/one-of [-float -string -bool -enum -integer])]
    (gen/return (str n ":" v))))

(def -arguments
  (gen/let [args (gen/vector -argument 1 3)]
    (gen/return
      (format "(%s)" (string/join ", " args)))))

(def -directive
  "Generate a single GraphQL directive."
  (gen/let [n -name
            a (rarely -arguments)]
    (gen/return (str "@" n a))))

(def -directives
  "Generate multiple GraphQL directives, separated by a space."
  (->> (gen/vector -directive 1 3)
       (gen/fmap #(string/join " " %))))

;; ## Type Definition

(def ^:private -type-definition-default-value
  (gen/one-of
    [-float
     -string
     -bool
     -enum
     -integer]))

(def ^:private -type-definition-argument
  (gen/let [n -name
            t -type
            d (maybe -type-definition-default-value)]
    (str n ": " t
         (if (not= (last t) \!)
           (some->> d (str " = "))))))

(def ^:private -type-definition-arguments
  (gen/let [a (gen/vector -type-definition-argument 1 3)]
    (str "(" (string/join ", " a) ")")))

(def ^:private -type-definition-field
  (gen/let [n -name
            a (maybe -type-definition-arguments)
            t -type]
    (str n a ": " t)))

(def ^:private -type-definition-fields
  (gen/let [f (gen/vector -type-definition-field 1 5)]
    (str "{" (string/join ", " f) "}")))

(def ^:private -type-implements
  (gen/let [is (gen/vector -type-name 1 3)]
    (str "implements " (string/join ", " is))))

(def -type-definition
  "Generate a valid GraphQL `type` definition."
  (gen/let [n -type-name
            i -type-implements
            d (rarely -directives)
            f -type-definition-fields]
    (str "type " n
         (some->> i (str " "))
         (some->> d (str " "))
         " "
         f)))

(def -type-extends-definition
  "Generate a valid GraphQL `extend type` definition."
  (gen/let [n -type-name
            i -type-implements
            f -type-definition-fields]
    (str "extend type " n
         (some->> i (str " "))
         " "
         f)))

;; ## Interface Definition

(def -interface-definition
  "Generate a valid GraphQL `interface` definition."
  (gen/let [n -type-name
            d (rarely -directives)
            f -type-definition-fields]
    (str "interface " n
         (some->> d (str " "))
         " "
         f)))

;; ## Input Types

(def ^:private -input-type-definition-field
  (gen/let [n -name
            t -type]
    (str n ": " t)))

(def -input-type-definition
  "Generate a valid GraphQL `input` definition."
  (gen/let [n -type-name
            d (rarely -directives)
            f (gen/vector -input-type-definition-field 1 5)]
    (str "input " n
         (some->> d (str " "))
         " {"
         (string/join ", " f)
         "}")))

;; ## Scalar Definition

(def -scalar-definition
  "Generate a valid GraphQL `scalar` definition."
  (gen/fmap #(str "scalar " %) -type-name))

;; ## Enum Definition

(def -enum-definition
  "Generate a valid GraphQL `enum` definition."
  (gen/let [vs (gen/vector
                 (gen/fmap string/upper-case -name)
                 1 4)
            d (rarely -directives)
            n -type-name]
    (str "enum " n
         (some->> d (str " "))
         " {"
         (string/join ", " vs)
         "}")))

;; ## Union Definition

(def -union-definition
  "Generate a valid GraphQL `union` definition."
  (gen/let [n -type-name
            d (rarely -directives)
            vs (gen/vector -type-name 1 5)]
    (str "union " n " = " (string/join " | " vs)
         (some->> d (str " ")))))

;; ## Directive Definition

(def -directive-definition
  "Generate a valid GraphQL `directive` definition."
  (gen/let [n -name
            l (gen/elements
                ["QUERY"
                 "MUTATION"
                 "SUBSCRIPTION"
                 "FIELD"
                 "FRAGMENT_DEFINITION"
                 "FRAGMENT_SPREAD"
                 "INLINE_FRAGMENT"
                 "SCHEMA"
                 "SCALAR"
                 "OBJECT"
                 "FIELD_DEFINITION"
                 "ARGUMENT_DEFINITION"
                 "INTERFACE"
                 "UNION"
                 "ENUM"
                 "ENUM_VALUE"
                 "INPUT_OBJECT"
                 "INPUT_FIELD_DEFINITION"])
            a (maybe -type-definition-arguments)]
    (str "directive @" n (some->> a (str " ")) " on " l)))

;; ## Schema Definition

(def -schema-definition
  "Generate a valid GraphQL `schema` definition."
  (gen/let [ks (gen/set
                 (gen/elements
                   ["query"
                    "mutation"
                    "subscription"])
                 {:min-elements 1
                  :max-elements 3})
            d (rarely -directives)
            vs (gen/vector -type-name (count ks))]
    (str "schema"
         (some->> d (str " "))
         " {"
         (->> (map #(str %1 ": " %2) ks vs)
              (string/join ", "))
         "}")))

;; ## Type System

(def -schema
  "Generate a valid GraphQL schema, including all possible definition types."
  (gen/let [schema-def (maybe -schema-definition)
            other-defs (-> (gen/one-of
                             [-type-definition
                              -type-extends-definition
                              -scalar-definition
                              -enum-definition
                              -interface-definition
                              -union-definition
                              -input-type-definition
                              -directive-definition])
                           (gen/vector 1 8))]
    (->> (cons schema-def other-defs)
         (filter identity)
         (string/join "\n"))))
