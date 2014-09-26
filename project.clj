(defproject cookiemonster "0.1.0-SNAPSHOT"
  :description "Take warm cookie notifications from Specialty's and Put them in Slack."
  :url "https://github.com/dreid/cookiemonster"
  :license {:name "MIT" :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha1"]
                 [clj-http "1.0.0"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [org.clojure/tools.logging "0.3.1"]]

  :plugins [[com.palletops/uberimage "0.2.0"]]

  :profiles {:uberjar {:aot :all}}
  :main cookiemonster.core

  :uberimage {:cmd ["/usr/bin/java" "-jar" "/uberjar.jar" "/config.edn"]
              :files {"config.edn" "config.edn"}})
