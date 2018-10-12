(ns kafka-clj-utils.healthcheck-test
  (:require [clojure.test :refer :all]
            [kafka-clj-utils.test-utils :refer [with-zookareg+app]]))

(deftest healthcheck-test

  (testing "healthcheck fails when unhealthy"
    (let [ig-config {:kafka-clj-utils.healthcheck/healthcheck
                     {:bootstrap.servers "no-such-host:9091"}}]
      (with-zookareg+app
        ig-config
        (fn [system]
          (let [healthcheck (:kafka-clj-utils.healthcheck/healthcheck system)]
            (is (= {:name "kafka" :healthy? false} (healthcheck))))))))

  (testing "healthcheck succeeds when healthy"
    (let [ig-config {:kafka-clj-utils.healthcheck/healthcheck
                     {:bootstrap.servers "localhost:9092"}}]
      (with-zookareg+app
        ig-config
        (fn [system]
          (let [healthcheck (:kafka-clj-utils.healthcheck/healthcheck system)]
            (is (= {:name "kafka" :healthy? true} (healthcheck)))))))))

