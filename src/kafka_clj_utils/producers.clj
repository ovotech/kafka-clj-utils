(ns kafka-clj-utils.producers
  (:require [abracad.avro :as avro]
            [clojure.spec.alpha :as s]
            [integrant.core :as ig]
            [kafka-avro-confluent.v2.serializer :as avro-serializer]
            [kafka-clj-utils.utils :as ku])
  (:import java.util.Map
           [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
           org.apache.kafka.common.serialization.StringSerializer))

(require 'kafka-clj-utils.specs)

(defn- valid-avro-schema? [x]
  (try
    (avro/parse-schema x)
    true
    (catch Exception _
      false)))
(s/def :avro/schema valid-avro-schema?)
(s/def ::avro-schema :avro/schema)

(s/def ::eventId ::ku/non-blank-string)
(s/def ::metadata (s/keys :req-un [::eventId]))
(s/def ::record (s/keys :req-un [::metadata]))
(s/def ::records (s/coll-of ::record))
(s/def :kafka/topic-name ::ku/non-blank-string)
(s/def ::avro-bundle
  (s/keys :req-un [::avro-schema ::records :kafka/topic-name]))

(s/fdef ->producer
        :args
        (s/cat :kafka/config :kafka/config
               :serde/config :serde/config))
(defn ^KafkaProducer ->producer
  [kafka-config serde-config]
  (let [producer-config (ku/normalize-config kafka-config)
        key-ser         (StringSerializer.)
        value-ser       (avro-serializer/->avro-serializer serde-config)]
    (KafkaProducer. ^Map producer-config key-ser value-ser)))

(defn- FailureTrackingCallback [failure-state record]
  (reify org.apache.kafka.clients.producer.Callback
    (onCompletion [_this _metadata ex]
      (when ex
        (reset! failure-state {:ex     ex
                               :record record})))))

(s/fdef publish-avro-bundle
        :args
        (s/cat :k-producer some?
               :avro-bundle ::avro-bundle))
(defn publish-avro-bundle
  "Atomically produces an ::avro-bundle, throwing if any of the sends failed."
  [k-producer
   {:keys [avro-schema topic-name records] :as _bundle}]
  (let [failure (atom nil)]
    ;; NOTE Allow `_` in keys:
    ;; https://github.com/damballa/abracad#basic-deserialization
    (with-bindings {#'abracad.avro.util/*mangle-names* false}
      (doseq [k-val (map (fn [v]
                           {:schema avro-schema
                            :value  v})
                         records)
              :let  [k-key (get-in k-val [:metadata :eventId])
                     failure-cbk (FailureTrackingCallback failure k-val)]]
        (.send k-producer
               (ProducerRecord. topic-name k-key k-val)
               failure-cbk)
        (.flush k-producer)
        (when-let [f @failure]
          (throw (ex-info "At least one of the `KafkaProducer::send`s failed!. One example:"
                          {:topic-name            topic-name
                           :failed-records-sample (:record f)}
                          (:ex f))))))))

(s/def ::bundle-publisher.opts
  (s/keys :req [:kafka/config
                :kafka.serde/config]))
(defmethod ig/pre-init-spec ::bundle-publisher
  [_]
  ::bundle-publisher.opts)

(defprotocol ^:private Closeable
  (close [this]))
(deftype ^:private BundlePublisher [k-producer produce-fn]
  Closeable
  (close [this]
    (.close k-producer))
  clojure.lang.IFn
  (invoke [this arg]
    (produce-fn arg)))
(alter-meta! #'->BundlePublisher assoc :private true)

(defmethod ig/init-key ::bundle-publisher
  [_ opts]
  (let [k-producer (->producer (:kafka/config opts)
                               (:kafka.serde/config opts))]
    (->BundlePublisher k-producer
                       (partial publish-avro-bundle k-producer))))

(defmethod ig/halt-key! ::bundle-publisher
  [_ bundle-publisher]
  (close bundle-publisher))
