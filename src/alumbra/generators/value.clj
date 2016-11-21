(ns alumbra.generators.value
  (:require [alumbra.generators.raw.value :as v]
            [clojure.test.check
             [generators :as gen]]
            [clojure.string :as string]))

;; ## Scalars

(def ^:private short-string
  "Generate a valid GraphQL string value."
  (->> (gen/vector gen/char-alphanumeric 1 16)
       (gen/fmap #(string/replace
                    (apply str %)
                    #"[\"\\]"
                    (fn [[match]]
                      (str "\\" match))))
       (gen/fmap #(str \" % \"))))

(defn- scalar-generator
  [{:keys [scalars]} {:keys [type-name]}]
  (or (get scalars type-name)
      (case type-name
        "ID"      short-string
        "String"  short-string
        "Boolean" v/-bool
        "Float"   v/-float
        "Int"     v/-integer)))

(defn- scalar-generators
  [{:keys [schema] :as opts}]
  (->> (for [[type-name scalar] (:scalars schema)]
         [type-name (scalar-generator opts scalar)])
       (into {})))

;; ## Complex Types

(defn- field-generator
  [{:keys [value-type->gen]} {:keys [field-name type-description]}]
  (->> (value-type->gen type-description)
       (gen/fmap #(str field-name ": " %))))

(defn- optional-field-generator
  [opts field]
  (gen/frequency
    [[5 (gen/return nil)]
     [5 (field-generator opts field)]]))

(defn- combine-fields
  [gens]
  (->> (apply gen/tuple gens)
       (gen/fmap #(filter some? %))
       (gen/fmap #(string/join ", " %))
       (gen/fmap #(str "{" % "}"))))

(defn- input-type-generator
  [opts {:keys [fields]}]
  (let [fields (vals fields)
        required-fields (set (filter :non-null? fields))
        optional-fields (set (remove required-fields fields))]
    (->> (concat
           (map #(field-generator opts %) required-fields)
           (map #(optional-field-generator opts %) optional-fields))
         (combine-fields))))

(defn- input-type-generators
  [{:keys [schema] :as opts}]
  (->> (for [[type-name input-type] (:input-types schema)]
         [type-name (input-type-generator opts input-type)])
       (into {})))

;; ## Enums

(defn- enum-generator
  [opts {:keys [enum-values]}]
  (gen/elements enum-values))

(defn- enum-generators
  [{:keys [schema] :as opts}]
  (->> (for [[type-name enum] (:enums schema)]
         [type-name (enum-generator opts enum)])
       (into {})))

;; ## Helper

(defn- nullable
  [gen]
  (gen/frequency
    [[1 (gen/return "null")]
     [9 gen]]))

;; ## Value Generator

(defn value-generators
  [opts]
  (let [gen-promise (promise)
        name->gen
        (fn f
          [{:keys [type-name non-null? type-description]}]
          (gen/bind
            (gen/return nil)
            (fn [_]
              (cond-> (if type-name
                        (or (get @gen-promise type-name)
                            (throw
                              (IllegalArgumentException.
                                (str "generator missing for type: " type-name))))
                        (->> (gen/vector (f type-description) 0 3)
                             (gen/fmap #(string/join ", " %))
                             (gen/fmap #(str "[" % "]"))))
                (not non-null?) nullable))))
        opts (assoc opts :value-type->gen name->gen)]
    (->> (merge
           (scalar-generators opts)
           (input-type-generators opts)
           (enum-generators opts))
         (deliver gen-promise))
    name->gen))
