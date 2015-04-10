(defproject org.zalando.stups/twintip-crawler "0.1.0-SNAPSHOT"
  :description "An API definition crawler."
  :url "https://github.com/zalando-stups/twintip"

  :license {:name "The Apache License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.0.0"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.zalando.stups/friboo "0.6.0-SNAPSHOT"]
                 [clj-http-lite "0.2.1"]]

  :main ^:skip-aot org.zalando.stups.twintip.crawler.core
  :uberjar-name "twintip-crawler.jar"

  :plugins [[io.sarnowski/lein-docker "1.1.0"]]

  :docker {:image-name "stups/twintip-crawler"}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["clean"]
                  ["uberjar"]
                  ["docker" "build"]
                  ["docker" "push"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :pom-addition [:developers
                 [:developer {:id "sarnowski"}
                  [:name "Tobias Sarnowski"]
                  [:email "tobias.sarnowski@zalando.de"]
                  [:role "Maintainer"]]]

  :profiles {:uberjar {:aot :all}

             :dev     {:repl-options {:init-ns user}
                       :source-paths ["dev"]
                       :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                      [org.clojure/java.classpath "0.2.2"]]}})
