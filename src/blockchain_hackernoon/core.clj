(ns blockchain-hackernoon.core
  (:gen-class)
  (:require [integrant.core :as ig]
            [clojure.java.io :as io]))

(defn -main [& args]
  (let [config (ig/read-string (slurp (io/resource "config.edn")))]
    (ig/load-namespaces config)
    (ig/init config)))