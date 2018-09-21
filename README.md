# kafka-clj-utils [![CircleCI](https://circleci.com/gh/ovotech/kafka-clj-utils/tree/master.svg?style=svg)](https://circleci.com/gh/ovotech/kafka-clj-utils/tree/master)

A number of utilities for dealing with Kafka + Avro Schema Registry.

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/ovotech/kafka-clj-utils.svg)](https://clojars.org/ovotech/kafka-clj-utils)

### Avro Bundle Producer

Multiple Avro encoded messages can be published to Kafka using `kafka-clj-utils.producerspublish-avro-bundle`

```clojure
(let [opts   {:kafka-config           {...}
              :schema-registry-client {...}}
      bundle {:avro-schema            {...}
              :topic-name             "my-topic"
              :records                [{...}
                                       {...}]}]
   (publish-avro-bundle opts bundle))
```

### Healthchecks

Two healthcheck functions are provided to confirm functionality of components:

* kafka - `kafka-clj-utils.healthcheck` checks connectivity to Kafka by listing topics
* schema-registry - `kafka-clj-utils.schema-registry` ensure connectivity to an Avro Schema Registry by retrieving the registry's config

### Integrant Keys

A number of [Integrant](https://github.com/weavejester/integrant) `init-key` multimethods are exposed to make 
including these utilities into an Integrant app easier.

All of the following keys are exposed with `pre-init-spec`s:

* kafka-clj-utils.healthcheck/healthcheck
* kafka-clj-utils.producers/bundle-publisher
* kafka-clj-utils.schema-registry/schema-registry-client
* kafka-clj-utils.schema-registry/healthcheck

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
