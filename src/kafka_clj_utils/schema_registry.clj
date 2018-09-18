(ns kafka-clj-utils.schema-registry
  (:require [integrant.core :as ig]
            [kafka-avro-confluent.schema-registry-client :as schema-registry]))

(defn healthcheck
  [schema-registry-client]
  {:name "schema-registry",
   :healthy? (deref (future (schema-registry/healthy? schema-registry-client))
                    2000 false)})

(defmethod ig/init-key ::client
  [_ opts]
  (schema-registry/->schema-registry-client opts))

(defmethod ig/init-key ::healthcheck [_ opts] #(healthcheck opts))
