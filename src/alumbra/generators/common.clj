(ns alumbra.generators.common
  (:require [clojure.test.check.generators :as gen]
            [clojure.string :as string]))

(defn ^:no-doc maybe
  [g]
  (gen/frequency
    [[1 (gen/return nil)]
     [99 g]]))

(defn ^:no-doc rarely
  [g]
  (gen/frequency
    [[8 (gen/return nil)]
     [2 g]]))

(def -name
  "Generate a valid GraphQL name token."
  (->> (gen/tuple
         gen/char-alpha
         gen/string-alphanumeric)
       (gen/fmap #(apply str %))))

(def -variable
  "Generate a valid GraphQL variable token."
  (gen/fmap #(str "$" %) -name))
