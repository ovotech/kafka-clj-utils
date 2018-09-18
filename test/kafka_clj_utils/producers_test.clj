(ns kafka-clj-utils.producers-test
  (:require [clojure.test :refer :all]))

(use-fixtures :once (partial tu/with-around-fns [tu/with-zookareg
                                                 (partial tu/with-topic topic-a)
                                                 (partial tu/with-topic topic-b)
                                                 (partial tu/with-ig-sys config)]))


(deftest not-implemented


  ;; Args for the producer
  #_[{:keys [kafka-config schema-registry-client] :as _opts}
   {:keys [avro-schema topic-name records] :as _bundle}]


  ;; So really we need to just send a schema over zookareg
  ;; then get use the utils to get something off Kafka?





  (is false))




