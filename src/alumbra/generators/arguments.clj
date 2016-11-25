(ns alumbra.generators.arguments
  (:require [clojure.test.check
             [generators :as gen]]
            [clojure.string :as string]))


(defn- argument-generator
  [{:keys [value-gen]} {:keys [argument-name type-description] :as x}]
  (gen/fmap #(str argument-name ": " %)
            (value-gen type-description)))

(defn- optional-argument-generator
  [opts argument-data]
  (gen/frequency
    [[95 (gen/return nil)]
     [5 (argument-generator opts argument-data)]]))

(defn- combine-argument-gens
  [gens]
  (if (seq gens)
    (->> (apply gen/tuple gens)
         (gen/fmap #(filter some? %))
         (gen/fmap
           (fn [args]
             (when (seq args)
               (str "(" (string/join ", " args) ")")))))
    (gen/return nil)))

(defn arguments-generator
  [opts name->argument]
  (let [arguments (vals name->argument)
        required-arguments (set (filter :non-null? arguments))
        optional-arguments (set (remove required-arguments arguments))]
    (->> (concat
           (map #(argument-generator opts %) required-arguments)
           (map #(optional-argument-generator opts %) optional-arguments))
         (combine-argument-gens))))
