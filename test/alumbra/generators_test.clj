(ns alumbra.generators-test
  (:require [clojure.test.check
             [properties :as prop]
             [clojure-test :refer [defspec]]]
            [alumbra.generators
             [document :refer [-document]]
             [schema :refer [-schema]]]))

(defspec t-document-generator 500
  (prop/for-all
    [s -document]
    (string? s)))

(defspec t-schema-generator 500
  (prop/for-all
    [s -schema]
    (string? s)))
