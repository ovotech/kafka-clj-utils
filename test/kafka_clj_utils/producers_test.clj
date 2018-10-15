(ns kafka-clj-utils.producers-test
  (:require [clojure.test :refer :all]
            [kafka-clj-test-utils.consumer :as ktc]
            [kafka-clj-utils.producers :as kp]
            [kafka-clj-utils.test-utils :refer [with-zookareg+app]]
            [zookareg.core :as zkr]))

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


(deftest produce-ig-keys-test
  (let [config    {:kafka.serde/config {:schema-registry/base-url "http://localhost:8081"}
                   :kafka/config       {:bootstrap.servers  "127.0.0.1:9092"
                                        :retries            100
                                        :acks               "all"
                                        :request.timeout.ms 12000
                                        :max.block.ms       10000}}
        ig-config {:kafka-clj-utils.producers/bundle-publisher config}
        rec-1     {:foo "FOO" :bar "BAR" :metadata {:eventId "key-a"}}
        rec-2     {:foo "BAZ" :bar "QUX" :metadata {:eventId "key-b"}}]

    (with-zookareg+app
      ig-config
      (fn [system]
        (let [publish     (:kafka-clj-utils.producers/bundle-publisher system)
              avro-bundle {:avro-schema test-schema
                           :topic-name  topic-a
                           :records     [rec-1 rec-2]}]

          (publish avro-bundle)

          (let [[msg1 msg2 :as msgs] (ktc/consume config topic-a :expected-msgs 2)]
            (is (= 2 (count msgs)))
            (is (= rec-1 msg1))
            (is (= rec-2 msg2))))))))

(deftest producing-fns-test
  (zkr/with-zookareg (zkr/read-default-config)
    (let [config     {:kafka.serde/config {:schema-registry/base-url "http://localhost:8081"}
                      :kafka/config       {:bootstrap.servers "127.0.0.1:9092"}}
          k-producer (kp/->producer config)
          bundle     {:avro-schema {:type   :record
                                    :name   "Greeting"
                                    :fields [{:name "greeting"
                                              :type "string"}]}
                      :topic-name  "my-topic"
                      :records     [{:greeting "hi"}
                                    {:greeting "hola"}
                                    {:greeting "bundi`"}]}]
      (kp/publish-avro-bundle k-producer bundle)
      (let [msgs (ktc/consume config
                              "my-topic"
                              :expected-msgs 3)]
        (is (= 3 (count msgs)))))))


(deftest producing-with-key-test
  (testing "by default the [:metadata :eventId] is used as the message key"
    (zkr/with-zookareg (zkr/read-default-config)
      (let [config     {:kafka.serde/config {:schema-registry/base-url "http://localhost:8081"}
                        :kafka/config       {:bootstrap.servers "127.0.0.1:9092"}}
            k-topic    "my-keyed-topic"
            k-producer (kp/->producer config)
            bundle     {:avro-schema {:type   :record
                                      :name   "Dummy"
                                      :fields [{:name "metadata"
                                                :type {:name   "Metadata"
                                                       :type   :record
                                                       :fields [{:name "eventId"
                                                                 :type "string"}] }}]}
                        :topic-name  k-topic
                        :records     [{:metadata {:eventId "1"}}
                                      {:metadata {:eventId "2"}}]}]
        (kp/publish-avro-bundle k-producer bundle)
        (let [msgs (ktc/consume config
                                k-topic
                                :expected-msgs 3)]
          (is (= 2 (count msgs)))
          (is (= ["1" "2"] (map (comp :kafka/key meta) msgs))))))))
