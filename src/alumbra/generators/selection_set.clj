(ns alumbra.generators.selection-set
  (:require [clojure.test.check
             [generators :as gen]]
            [clojure.string :as string]))

;; ## Generic Generators

;; ### Helper

(defn- unique-gen
  [element-gen]
  (->> (gen/set element-gen {:min-elements 0, :max-elements 3})
       (gen/fmap #(filterv some? %))))

;; ### Arguments

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

(defn- arguments-generator
  [opts name->argument]
  (let [arguments (vals name->argument)
        required-arguments (set (filter :non-null? arguments))
        optional-arguments (set (remove required-arguments arguments))]
    (->> (concat
           (map #(argument-generator opts %) required-arguments)
           (map #(optional-argument-generator opts %) optional-arguments))
         (combine-argument-gens))))

;; ### Fields

(defn- field-generator
  [{:keys [type-name->gen] :as opts}
   {:keys [field-name type-name arguments]}]
  (let [arguments-gen (arguments-generator opts arguments)]
    (->> (gen/tuple
           (type-name->gen type-name)
           arguments-gen)
         (gen/fmap
           (fn [[selection-set arguments]]
             (str field-name
                  arguments
                  (when (seq selection-set)
                    (str " " selection-set))))))))

(defn- is-complex?
  [{:keys [schema]} type-name]
  (contains? #{:type :union :interface}
             (get-in schema [:type->kind type-name])))

(defn- weighted-field-selector
  [opts field-name->gen]
  (let [possible-fields (keys field-name->gen)
        complex-fields (set (filter #(is-complex? opts %) possible-fields))
        leaf-fields (set (remove complex-fields possible-fields))]
    (cond (and (seq complex-fields) (seq leaf-fields))
          (gen/frequency
            [[90 (gen/elements (disj leaf-fields "__typename"))]
             [2 (gen/return "__typename")]
             [8 (gen/elements complex-fields)]])

          (next leaf-fields)
          (gen/frequency
            [[98 (gen/elements (disj leaf-fields "__typename"))]
             [2 (gen/return "__typename")]])

          (seq leaf-fields)
          (gen/frequency
            [[1 (gen/return (first leaf-fields))]
             [1 (gen/return nil)]])

          :else
          (gen/elements complex-fields))))

;; ### Inline Spread

(defn- spread-generator
  [{:keys [type-name->gen]} spread-type-name]
  (->> (type-name->gen spread-type-name)
       (gen/fmap #(str "... on " spread-type-name " " %))))

(defn- weighted-spread-selector
  [_ spread-name->gen]
  (if (seq spread-name->gen)
    (gen/frequency
      [[9 (gen/return nil)]
       [1 (gen/elements (keys spread-name->gen))]])
    (gen/return nil)))

;; ### Selection Set

(defn- select-generators
  [name->gen name-gen]
  (if (seq name->gen)
    (gen/let [names (unique-gen name-gen)]
      (apply gen/tuple (map name->gen names)))
    (gen/return [])))

(defn- selection-set-generator*
  [opts spread-name->gen field-name->gen]
  (let [field-name-gen  (weighted-field-selector opts field-name->gen)
        spread-name-gen (weighted-spread-selector opts spread-name->gen)
        fields-gen      (select-generators field-name->gen field-name-gen)
        spreads-gen     (select-generators spread-name->gen spread-name-gen)
        elements-gen    (->> (gen/tuple fields-gen spreads-gen)
                             (gen/fmap #(apply concat %))
                             (gen/such-that seq))]
    (->> elements-gen
         (gen/fmap #(string/join ", " %))
         (gen/fmap #(str "{ " % " }")))))

(defn- selection-set-generator
  [opts {:keys [type-name fields valid-fragment-spreads]}]
  (let [field-name->gen
        (->> (for [[field-name field] fields]
               [field-name (field-generator opts field)])
             (into {}))
        spread-name->gen
        (->> (for [spread-name (disj valid-fragment-spreads type-name)]
               [spread-name (spread-generator opts spread-name)])
             (into {}))]
    (selection-set-generator* opts spread-name->gen field-name->gen)))

;; ## Generators by Kind

;; ### Object

(defn- build-object-generators
  [{:keys [schema] :as opts}]
  (->> (for [[type-name object] (:types schema)]
         [type-name (selection-set-generator opts object)])
       (into {})))

;; ### Interface

(defn- build-interface-generators
  [{:keys [schema] :as opts}]
  (->> (for [[type-name interface] (:interfaces schema)]
         [type-name (selection-set-generator opts interface)])
       (into {})))

;; ### Union

(defn- build-union-generators
  [{:keys [schema] :as opts}]
  (->> (for [[type-name union] (:unions schema)]
         [type-name (selection-set-generator opts union)])
       (into {})))

;; ## Selection Set

(defn selection-set-generators
  "Generate a function that, given a type name, produces a generator for said
   type's selection set."
  [{:keys [schema] :as opts}]
  (let [gen-promise (promise)
        type-name->gen (fn [type-name]
                         (gen/bind
                           (gen/return nil)
                           (fn [_]
                             (or (get @gen-promise type-name)
                                 (gen/return "")))))
        opts (assoc opts :type-name->gen type-name->gen)]
    (->> (merge
           (build-object-generators opts)
           (build-interface-generators opts)
           (build-union-generators opts))
         (deliver gen-promise))
    type-name->gen))
