(ns alumbra.generators.raw.value
  (:require [clojure.test.check.generators :as gen]
            [alumbra.generators.raw.common :refer :all]
            [clojure.string :as string]))

(def -integer
  "Generate a valid GraphQL integer value."
  gen/int)

(def ^:private -digits
  (as-> "0123456789" <>
    (gen/elements <>)
    (gen/vector <> 1 8)
    (gen/fmap #(apply str %) <>)))

(def ^:private -exponent
  (gen/let [digits -digits
            sign   (maybe (gen/elements ["+" "-"]))
            e      (gen/elements ["e" "E"])]
    (gen/return (str e sign digits))))

(def -float
  "Generate a valid GraphQL float value."
  (->> (gen/one-of
         [(gen/fmap #(str "." %) -digits)
          -exponent
          (gen/let [e -exponent
                    d -digits]
            (gen/return (str "." d e)))])
       (gen/tuple -integer)
       (gen/fmap #(apply str %))))

(def -string
  "Generate a valid GraphQL string value."
  (->> (gen/not-empty gen/string-ascii)
       (gen/fmap #(string/replace
                    %
                    #"[\"\\]"
                    (fn [[match]]
                      (str "\\" match))))
       (gen/fmap #(str \" % \"))))

(def -bool
  "Generate a valid GraphQL boolean value."
  (gen/elements ["true" "false"]))

(def -enum
  "Generate a valid GraphQL enum value."
  (->> (gen/such-that #(not (#{"true" "false" "null"} %)) -name)
       (gen/fmap string/upper-case)))

(defn -list
  "Generate a valid GraphQL list using the given generator for its elements."
  [g]
  (gen/let [vs (gen/vector g 0 5)]
    (gen/return
      (format "[%s]" (string/join ", " vs)))))

(defn -object
  "Generate a valid GraphQL object using the given generator for keys/values."
  [g]
  (let [field-gen (gen/let [n -name, v g]
                    (gen/return (str n ": " v)))]
    (gen/let [fields (gen/vector field-gen 0 3)]
      (gen/return
        (format "{%s}" (string/join ", " fields))))))

(def -null
  "Generate the GraphQL 'null' value."
  (gen/return "null"))

(let [wrap #(gen/recursive-gen
              (fn [g]
                (gen/frequency
                  [[10 (-list g)]
                   [10 (-object g)]
                   [80 g]]))
              %)]
  (def -value
    "Generate a valid GraphQL value (or variable)."
    (wrap
      (gen/one-of
        [-variable
         -integer
         -float
         -string
         -bool
         -enum
         -null])))

  (def -const
    "Generate a valid GraphQL value (no variables)."
    (wrap
      (gen/one-of
        [-integer
         -float
         -string
         -bool
         -enum
         -null]))))
