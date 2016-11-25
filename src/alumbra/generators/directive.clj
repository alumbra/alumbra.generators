(ns alumbra.generators.directive
  (:require [alumbra.generators.arguments
             :refer [arguments-generator]]
            [clojure.test.check
             [generators :as gen]]
            [clojure.string :as string]))

(defn- directive-generator
  [opts directive-name {:keys [arguments]}]
  (gen/fmap
    (fn [arguments]
      (str "@" directive-name arguments))
    (arguments-generator opts arguments)))

(defn- combine-generators
  [gens]
  (gen/let [gs (gen/fmap set (gen/vector (gen/elements gens) 0 2))]
    (some->> gs (seq) (gen/one-of))))

(defn directive-generators
  "Generate a function that, given a directive location, creates directives
   for said location (or `nil`)."
  [{:keys [schema] :as opts}]
  (let [loc->gen (->> (for [[directive-name directive] (:directives schema)
                            :let  [gen (directive-generator opts directive-name directive)]
                            loc (:directive-locations directive)]
                        [loc gen])
                      (group-by first)
                      (map
                        (fn [[loc gens]]
                          [loc (combine-generators (map second gens))]))
                      (into {}))]
    (fn [directive-location]
      (or (loc->gen directive-location)
          (gen/return nil)))))
