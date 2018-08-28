(ns blockchain-hackernoon.app
  (:require [integrant.core :as ig]
            [blockchain-hackernoon.chain :refer [->Block]])
  (:import (java.util UUID)))

(def node-config {:identifier (UUID/randomUUID)
           :nodes #{}})

(defmethod ig/init-key :blockchain-hackernoon.app/storage [_ _]
  {:chain        (ref [(->Block 0 0 [] 100 1)])
   :transactions (ref [])})

(defmethod ig/halt-key! :blockchain-hackernoon.app/storage [_ {:keys [chain transactions]}]
  (dosync
    (ref-set chain [])
    (ref-set transactions [])))

(defmethod ig/init-key :blockchain-hackernoon.app/nodes [_ _]
  (atom node-config))

(defmethod ig/halt-key! :blockchain-hackernoon.app/nodes [_ nodes]
  (reset! nodes node-config))


