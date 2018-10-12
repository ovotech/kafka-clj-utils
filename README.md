# kafka-clj-utils [![CircleCI](https://circleci.com/gh/ovotech/kafka-clj-utils/tree/master.svg?style=svg)](https://circleci.com/gh/ovotech/kafka-clj-utils/tree/master)

A number of utilities for dealing with Kafka + Avro Schema Registry.

## Usage

[![Clojars
Project](https://img.shields.io/clojars/v/ovotech/kafka-clj-utils.svg)](https://clojars.org/ovotech/kafka-clj-utils)

### Avro Bundle Producer

Multiple Avro encoded messages can be published to Kafka using
`kafka-clj-utils.producers/publish-avro-bundle`

```clojure

(require '[kafka-clj-utils.producers :as kp])

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
  (kp/publish-avro-bundle k-producer bundle))
```

### Healthchecks

Two healthcheck functions are provided to confirm functionality of components:

* kafka - `kafka-clj-utils.healthcheck` checks connectivity to Kafka by listing
  topics
* schema-registry - `kafka-clj-utils.schema-registry` ensure connectivity to an
  Avro Schema Registry by retrieving the registry's config

### Integrant Keys

A number of [Integrant](https://github.com/weavejester/integrant) `init-key`
multimethods are exposed to make including these utilities into an Integrant app
easier.

All of the following keys are exposed with `pre-init-spec`s:

* `:kafka-clj-utils.healthcheck/healthcheck`
* `:kafka-clj-utils.producers/bundle-publisher`
* `:kafka-clj-utils.schema-registry/healthcheck`

## License

Copyright © 2018 OVO Energy Ltd.

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
