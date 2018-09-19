(ns kafka-clj-utils.healthcheck-test
  (:require [clojure.test :refer :all]
            [kafka-clj-utils.test-utils :refer [with-zookareg+app read-config]]))


(deftest not-implemented

  (testing "healthcheck fails when unhealthy"
    (let [ig-config (-> (read-config)
                        (dissoc :kafka-config)
                        (assoc-in [:kafka-clj-utils.healthcheck/healthcheck :bootstrap.servers ] "http://localhost:8082"))]
      (with-zookareg+app
        ig-config
        (fn [system]
          (let [healthcheck (:kafka-clj-utils.healthcheck/healthcheck system)]
            (is (= {:name "kafka" :healthy? false} (healthcheck))))))))

  (testing "healthcheck succeeds when healthy"
    (let [ig-config (dissoc (read-config) :kafka-config)]
      (with-zookareg+app
        ig-config
        (fn [system]
          (let [healthcheck (:kafka-clj-utils.healthcheck/healthcheck system)]
            (is (= {:name "kafka" :healthy? true} (healthcheck)))))))))

