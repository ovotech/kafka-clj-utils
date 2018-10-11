(ns kafka-clj-utils.schema-registry
  (:require [integrant.core :as ig]
            [kafka-avro-confluent.v2.schema-registry-client :as schema-registry]))

(require 'kafka-clj-utils.specs)

(defn healthcheck
  [schema-registry-client]
  {:name "schema-registry",
   :healthy? (deref (future (schema-registry/healthy? schema-registry-client))
                    2000
                    false)})

(defmethod ig/pre-init-spec ::healthcheck
  [_]
  :kafka.schema-registry/config)
(defmethod ig/init-key ::healthcheck
  [_ opts]
  #(healthcheck (schema-registry/->schema-registry-client opts)))
