(ns kafka-clj-utils.utils
  (:require [clojure.walk :refer [stringify-keys]]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]))


(s/def ::non-blank-string
  (s/and string? (complement string/blank?)))


(defn normalize-config
  "Normalise configuration map into a format required by kafka."
  [config]
  (->> config
       stringify-keys
       (map (fn [[k v]] [k (if (integer? v) (int v) v)]))
       (into {})))
