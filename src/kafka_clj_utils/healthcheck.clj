(ns kafka-clj-utils.healthcheck
  (:require [clojure.tools.logging :as log]
            [kafka-clj-utils.utils :refer [normalize-config]]
            [kafka-clj-utils.producers :as p]
            [integrant.core :as ig])
  (:import java.util.Map
           org.apache.kafka.clients.consumer.KafkaConsumer
           org.apache.kafka.common.serialization.StringDeserializer))

(defn ->healthcheck
  [kafka-config]
  (fn []
    {:name "kafka",
     :healthy? (boolean
                 (try (let [config (-> kafka-config
                                       (merge {:request.timeout.ms 12000})
                                       normalize-config)
                            key-serializer (StringDeserializer.)
                            value-serializer (StringDeserializer.)]
                        (with-open [c (KafkaConsumer. ^Map config
                                                      key-serializer
                                                      value-serializer)]
                          (.listTopics c)))
                      (catch Exception ex
                        (log/error ex "Kafka healthcheck failed")
                        false)))}))


(defmethod ig/pre-init-spec ::healthcheck
  [_]
  ::p/kafka-config)

(defmethod ig/init-key ::healthcheck
  [_ kafka-config]
  (->healthcheck kafka-config))
