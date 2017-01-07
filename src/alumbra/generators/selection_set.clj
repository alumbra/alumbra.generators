(ns alumbra.generators.selection-set
  (:require [alumbra.generators.arguments
             :refer [arguments-generator]]
            [alumbra.generators.raw
             [fragments :refer [-fragment-name]]]
            [clojure.test.check
             [generators :as gen]]
            [stateful.core :as stateful]
            [clojure.string :as string]))

;; ## Generic Generators

;; ### Helper

(defn- unique-gen
  [element-gen]
  (->> (gen/set element-gen {:min-elements 0, :max-elements 3})
       (gen/fmap #(filterv some? %))))

;; ### Fields

(defn- field-generator
  [{:keys [type-name->gen directive-gen] :as opts}
   {:keys [field-name type-name arguments]}]
  (let [arguments-gen (arguments-generator opts arguments)]
    (->> (gen/tuple
           (type-name->gen type-name)
           arguments-gen
           (directive-gen :field))
         (gen/fmap
           (fn [[selection-set arguments directives]]
             (str field-name
                  arguments
                  (some->> directives (str " "))
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
  [{:keys [type-name->gen directive-gen]} spread-type-name]
  (->> (gen/tuple
         (type-name->gen spread-type-name)
         (directive-gen :inline-fragment))
       (gen/fmap
         (fn [[selection-set directives]]
           (str "... on " spread-type-name
                (some->> directives (str " "))
                " " selection-set)))))

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

(defn- fragment-generator
  [gen type-name]
  ;; Instead of returning the selection set directly, we decide on a fragment
  ;; name and put the selection set into the generator state, appending it
  ;; later on the top-level.
  (gen/let [selection-set  gen
            free-name?     (gen/fmap (comp complement set)
                                     (stateful/value [:fragment-names]))
            fragment-name  (gen/such-that free-name? -fragment-name 100)]
    (stateful/return*
      (str "{ ..." fragment-name " }")
      (fn [state]
        (-> state
            (update :fragments (fnil conj [])
                    (str "fragment " fragment-name
                         " on " type-name
                         " " selection-set))
            (update :fragment-names (fnil conj #{}) fragment-name))))))

(defn- maybe-fragment-generator
  [gen type-name]
  (gen/frequency
    [[95 gen]
     [5 (fragment-generator gen type-name)]]))

(defn selection-set-generators
  "Generate a function that, given a type name, produces a generator for said
   type's selection set."
  [{:keys [schema] :as opts}]
  (let [gen-promise (promise)
        type-name->gen (fn [type-name]
                         (gen/bind
                           (gen/return nil)
                           (fn [_]
                             (or (some-> (get @gen-promise type-name)
                                         (maybe-fragment-generator type-name))
                                 (gen/return "")))))
        opts (assoc opts :type-name->gen type-name->gen)]
    (->> (merge
           (build-object-generators opts)
           (build-interface-generators opts)
           (build-union-generators opts))
         (deliver gen-promise))
    type-name->gen))
