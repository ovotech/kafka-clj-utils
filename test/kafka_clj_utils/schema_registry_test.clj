(ns kafka-clj-utils.schema-registry-test
  (:require [clojure.test :refer :all]
            [kafka-clj-utils.test-utils :refer [with-zookareg+app read-config]]
            [zookareg.core :as zkr]))


(deftest healthcheck
  (testing "healthcheck fails when unhealthy"
    (let [config (read-config)
          ig-config (-> config
                        (dissoc :kafka-config)
                        (assoc-in [:kafka-clj-utils.schema-registry/client :base-url] "http://localhost:8082"))]
      (with-zookareg+app
        ig-config
        (fn [system]
          (let [healthcheck (:kafka-clj-utils.schema-registry/healthcheck system)]
            (is (= {:name "schema-registry" :healthy? false} (healthcheck))))))))

  (testing "healthcheck succeeds when healthy"
    (let [config    (read-config)
          ig-config (dissoc config :kafka-config)]
      (with-zookareg+app
        ig-config
        (fn [system]
          (let [healthcheck (:kafka-clj-utils.schema-registry/healthcheck system)]
            (is (= {:name "schema-registry" :healthy? true} (healthcheck)))))))))