(defproject kafka-clj-utils "0.1.0-1"
  :description "Clojure utilities for Kafka"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[integrant "0.6.3"]
                 [org.apache.kafka/kafka-clients "1.0.2" :exclusions [org.scala-lang/scala-library]]
                 [org.apache.kafka/kafka-streams "1.0.2"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/spec.alpha "0.2.168"]
                 [org.clojure/tools.logging "0.4.1"]
                 [ovotech/kafka-avro-confluent "0.8.5"]]

  :profiles {:dev {:dependencies [[vise890/multistub "0.1.1"]
                                  [kafka-clj-test-utils "0.1.0-1"]]}})
