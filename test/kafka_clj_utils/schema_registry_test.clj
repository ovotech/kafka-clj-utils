(ns kafka-clj-utils.schema-registry-test
  (:require [clojure.test :refer :all]
            [kafka-clj-utils.test-utils :refer [with-zookareg+app]]))

(deftest healthcheck-test
  (testing "healthcheck fails when unhealthy"
    (let [ig-config {:kafka-clj-utils.schema-registry/healthcheck
                     {:schema-registry/base-url "http://no-such-host:8000"}}]
      (with-zookareg+app
        ig-config
        (fn [system]
          (let [healthcheck (:kafka-clj-utils.schema-registry/healthcheck system)]
            (is (= {:name "schema-registry" :healthy? false} (healthcheck))))))))

  (testing "healthcheck succeeds when healthy"
    (let [ig-config {:kafka-clj-utils.schema-registry/healthcheck
                     {:schema-registry/base-url "http://localhost:8081"}}]
      (with-zookareg+app
        ig-config
        (fn [system]
          (let [healthcheck (:kafka-clj-utils.schema-registry/healthcheck system)]
            (is (= {:name "schema-registry" :healthy? true} (healthcheck)))))))))
