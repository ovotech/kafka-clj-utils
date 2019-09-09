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
         :kafka.serde/config :kafka.serde/config))
(defn ^KafkaProducer ->producer
  ([config]
   (->producer (:kafka/config config) (:kafka.serde/config config)))
  ([kafka-config serde-config]
   (let [producer-config (ku/normalize-config kafka-config)
         key-ser         (StringSerializer.)
         value-ser       (avro-serializer/->avro-serializer serde-config)]
     (KafkaProducer. ^Map producer-config key-ser value-ser))))

(defn- ->callback [failure-state ack-callback-fn record]
  (reify org.apache.kafka.clients.producer.Callback
    (onCompletion [_this metadata ex]
      (if ex
        (reset! failure-state ex)
        (ack-callback-fn {:metadata metadata :record record})))))

(defn- assert-not-failed! [failure-state topic-name]
  (when-let [fail @failure-state]
    (throw
     (ex-info
      "At least one of the `KafkaProducer::send`s failed!"
      {:topic-name topic-name}
      fail))))

(s/fdef publish-avro-bundle
  :args
  (s/cat :k-producer some?
         :avro-bundle ::avro-bundle))

(defn publish-avro-bundle
  "Atomically produces an ::avro-bundle, throwing if any of the sends failed."
  ([k-producer bundle]
   (publish-avro-bundle k-producer (constantly nil) bundle))
  ([k-producer
    ack-callback-fn
    {:keys [avro-schema topic-name records] :as _bundle}]
   ;; NOTE Do not mess with names, logical types, etc.
   ;; https://github.com/damballa/abracad#basic-deserialization
   (binding [abracad.avro.util/*mangle-names* false]
     (let [avro-schema (avro/parse-schema avro-schema)
           failure     (atom nil)]
       (doseq [r    records
               :let [k-key (get-in r [:metadata :eventId])
                     k-val {:schema avro-schema
                            :value  r}]]
         (.send k-producer
                (ProducerRecord. topic-name k-key k-val)
                (->callback failure ack-callback-fn r))
         (assert-not-failed! failure topic-name))
       (.flush k-producer)
       (assert-not-failed! failure topic-name)))))

(s/def ::bundle-publisher.opts
  (s/keys :req [:kafka/config
                :kafka.serde/config]))
(defmethod ig/pre-init-spec ::bundle-publisher
  [_]
  ::bundle-publisher.opts)

(defmethod ig/init-key ::bundle-publisher
  [_ opts]
  (let [k-producer (->producer (:kafka/config opts)
                               (:kafka.serde/config opts))
        ack-callback-fn (get opts :ack-callback-fn (constantly nil))]
    (with-meta (partial publish-avro-bundle k-producer ack-callback-fn)
      {:k-producer k-producer})))

(defmethod ig/halt-key! ::bundle-publisher
  [_ bundle-publisher]
  (-> bundle-publisher
      meta
      :k-producer
      .close))
