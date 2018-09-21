(ns kafka-clj-utils.test-utils
  (:require [integrant.core :as ig]
            [clojure.java.io :as io]
            [aero.core :as aero]
            [zookareg.core :as zkr]))

(defn read-config []
  (-> "config.edn"
      io/resource
      aero/read-config))


(defn- with-ig-sys+
  [ig-config f]
  (let [_ (ig/load-namespaces ig-config)
        system (ig/init ig-config)]
    (try
      (f system)
      (finally (ig/halt! system)))))


(defn with-zookareg+app
  [ig-config f]
  (zkr/with-zookareg-fn
    (fn []
      (with-ig-sys+ ig-config f))))