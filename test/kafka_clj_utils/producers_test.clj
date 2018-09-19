(ns kafka-clj-utils.producers-test
  (:require [clojure.test :refer :all]
            [kafka-clj-test-utils.core :refer [with-topic]]
            [kafka-clj-utils.test-utils :refer [with-zookareg+app read-config]]
            [kafka-clj-test-utils.consumer :as ktc]))

(def topic-a "test.topic.a")

(def test-schema
  {:namespace "kafkaCljUtils"
   :type      "record"
   :name      "TestRecord"
   :fields    [{:name "metadata"
                :type {:type "record"
                       :name "metaV1"
                       :fields [{:name "eventId" :type "string"}]}}
               {:name "foo" :type "string"}
               {:name "bar" :type "string"}]})


(deftest produce
  (let [config (read-config)
        ig-config (dissoc config :kafka-config)

        rec-1 {:foo "FOO" :bar "BAR" :metadata {:eventId "key-a"}}
        rec-2 {:foo "BAZ" :bar "QUX" :metadata {:eventId "key-b"}}]

    (with-zookareg+app
      ig-config
      (fn [system]
        (let [publish (:kafka-clj-utils.producers/bundle-publisher system)
              avro-bundle {:avro-schema test-schema
                           :topic-name  topic-a
                           :records     [rec-1 rec-2]}]

          (publish avro-bundle)

          (let [[msg1 msg2 :as msgs] (ktc/consume config topic-a :expected-msgs 2)]
            (is (= 2 (count msgs)))
            (is (= rec-1 msg1))
            (is (= rec-2 msg2))))))))




