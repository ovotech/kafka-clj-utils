(ns kafka-clj-utils.specs
  (:require [clojure.spec.alpha :as s]))

(require 'kafka-avro-confluent.v2.specs)

(s/def ::bootstrap.servers string?)
(s/def :kafka/config (s/keys :req-un [::bootstrap.servers]))

(s/def :kafka.schema-registry/config :serde/config)
(s/def :kafka.serde/config :serde/config)
