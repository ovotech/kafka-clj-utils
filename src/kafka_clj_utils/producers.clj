(ns kafka-clj-utils.producers
  (:require [abracad.avro :as avro]
            [clojure.spec.alpha :as s]
            [kafka-clj-utils.utils :as ku]
            [kafka-clj-utils.schema-registry :as sr]
            [integrant.core :as ig]
            [kafka-avro-confluent.serializers :as avro-serializer])
  (:import java.util.Map
           [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
           org.apache.kafka.common.serialization.StringSerializer))

(s/def ::bootstrap.servers string?)
(s/def ::kafka-config (s/keys :req-un [::bootstrap.servers]))

(defn valid-avro-schema? [x]
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
  (s/cat :kafka-config ::kafka-config
         :schema-registry-client any?
         :avro-schema ::avro-schema))
(defn ^KafkaProducer ->producer
  [kafka-config schema-registry-client avro-schema]
  (let [producer-config (ku/normalize-config kafka-config)
        key-ser (StringSerializer.)
        value-ser (avro-serializer/->avro-serializer schema-registry-client
                                                     avro-schema)]
    (KafkaProducer. ^Map producer-config key-ser value-ser)))

(defn FailureTrackingCallback [failure-state record]
  (reify org.apache.kafka.clients.producer.Callback
    (onCompletion [_this _metadata ex]
      (when ex
        (reset! failure-state {:ex     ex
                               :record record})))))


(s/fdef publish-avro-bundle
        :args
        (s/cat :opts map?
               :avro-bundle ::avro-bundle))
(defn publish-avro-bundle
  [{:keys [kafka-config schema-registry-client] :as _opts}
   {:keys [avro-schema topic-name records] :as _bundle}]
  (let [k-producer  (->producer kafka-config schema-registry-client avro-schema)
        failure     (atom nil)]
    (try
      ;; NOTE Allow `_` in keys:
      ;; https://github.com/damballa/abracad#basic-deserialization
      (with-bindings {#'abracad.avro.util/*mangle-names* false}
        (doseq [k-val records
                :let  [k-key (get-in k-val [:metadata :eventId])
                       failure-cbk (FailureTrackingCallback failure k-val)]]
          (.send k-producer
                 (ProducerRecord. topic-name k-key k-val)
                 failure-cbk)
          (when-let [f  @failure]
            (throw (ex-info "At least one of the `KafkaProducer::send`s failed!. One example:"
                            {:topic-name topic-name
                             :failed-records-sample (:record f)}
                            (:ex f))))))
      (finally
        (.close k-producer)))))


(s/def ::bundle-publisher.opts
  (s/keys :req-un [::kafka-config
                   ::sr/schema-registry-client]))

(defmethod ig/pre-init-spec ::bundle-publisher
  [_]
  ::bundle-publisher.opts)

(defmethod ig/init-key ::bundle-publisher
  [_ opts]
  (partial publish-avro-bundle opts))

