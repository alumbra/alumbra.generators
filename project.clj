(defproject alumbra/generators "0.1.2-SNAPSHOT"
  :description "GraphQL Generators for Clojure's test.check"
  :url "https://github.com/alumbra/alumbra.generators"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2016
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha13" :scope "provided"]
                 [org.clojure/test.check "0.9.0" :scope "provided"]]
  :profiles {:codox
             {:plugins [[lein-codox "0.10.0"]]
              :dependencies [[codox-theme-rdash "0.1.1"]]
              :codox {:project {:name "alumbra.generators"}
                      :metadata {:doc/format :markdown}
                      :themes [:rdash]
                      :source-uri "https://github.com/alumbra/alumbra.generators/blob/v{version}/{filepath}#L{line}"}}}
  :aliases {"codox" ["with-profile" "+codox" "codox"]}
  :pedantic? :abort)
