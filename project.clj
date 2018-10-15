(defproject ovotech/kafka-clj-utils "1.1.1-2"
  :description "Clojure utilities for Kafka"
  :url "https://github.com/ovotech/kafka-clj-utils"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[integrant "0.7.0"]
                 [org.apache.kafka/kafka-clients "1.1.1" :exclusions [org.scala-lang/scala-library]]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [org.clojure/tools.logging "0.4.1"]
                 [ovotech/kafka-avro-confluent "1.1.1-1"]]

  :profiles {:dev {:dependencies   [[vise890/multistub "0.1.1"]
                                    [ovotech/kafka-clj-test-utils "1.1.1-1"]
                                    [ch.qos.logback/logback-classic "1.2.3"]
                                    [ch.qos.logback/logback-core "1.2.3"]]
                   :resource-paths ["dev/resources"]}
             :ci  {:deploy-repositories
                   [["clojars" {:url           "https://clojars.org/repo"
                                :username      :env ;; LEIN_USERNAME
                                :password      :env ;; LEIN_PASSWORD
                                :sign-releases false}]]}})
