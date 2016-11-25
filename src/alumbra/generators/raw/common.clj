(ns alumbra.generators.raw.common
  (:require [clojure.test.check.generators :as gen]
            [clojure.string :as string]))

(defn ^:no-doc maybe
  [g]
  (gen/frequency
    [[1 (gen/return nil)]
     [9 g]]))

(defn ^:no-doc rarely
  [g]
  (gen/frequency
    [[8 (gen/return nil)]
     [2 g]]))

(def -name
  "Generate a valid GraphQL name token."
  (gen/let [first-char gen/char-alpha
            rest-chars (gen/vector gen/char-alphanumeric 0 8)]
    (apply str first-char rest-chars)))

(def -variable
  "Generate a valid GraphQL variable token."
  (gen/fmap #(str "$" %) -name))
