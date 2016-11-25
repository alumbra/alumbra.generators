(defproject alumbra/generators "0.2.3-SNAPSHOT"
  :description "GraphQL Generators for Clojure's test.check"
  :url "https://github.com/alumbra/alumbra.generators"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2016
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [org.clojure/test.check "0.9.0" :scope "provided"]]
  :profiles {:dev
             {:dependencies [[alumbra/parser "0.1.1"]
                             [alumbra/analyzer "0.1.0"]]}
             :codox
             {:plugins [[lein-codox "0.10.0"]]
              :dependencies [[codox-theme-rdash "0.1.1"]]
              :codox {:project {:name "alumbra.generators"}
                      :metadata {:doc/format :markdown}
                      :themes [:rdash]
                      :namespaces [alumbra.generators]
                      :source-uri "https://github.com/alumbra/alumbra.generators/blob/v{version}/{filepath}#L{line}"}}}
  :aliases {"codox" ["with-profile" "+codox" "codox"]}
  :pedantic? :abort)
