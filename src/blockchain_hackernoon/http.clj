(ns blockchain-hackernoon.http
  (:require [yada.yada :refer [listener resource as-resource]]
            [blockchain-hackernoon.chain :refer [add-transaction! add-node! mine! resolve-conflict replace-chain!]]
            [integrant.core :as ig]
            [schema.core :as s]))

(defn mine-resource [storage nodes]
  (resource
    {:id          ::mine
     :description "Calculate the Proof of Work. Reward the miner (us) by adding a transaction granting us 1 coin. Forge the new Block by adding it to the chain"
     :summary     "Mines Block"
     :produces    {:media-type "application/json"}
     :methods     {:get
                   {:response (fn [_]
                                (let [new-block (mine! storage @nodes)]
                                  {:message "New block has been forged!" :block new-block}))}}}))

(s/def Amount (s/both (s/pred number?) (s/pred pos?)))

(s/defschema Transaction
           {:sender String
            :recipient String
            :amount Amount})

(s/def NodeAddress (s/pred #(re-matches #"http://(.*):\d+" %)))

(defn new-transaction-resource [storage]
  (resource
    {:id          ::transaction
     :description "Create a new transaction to/from with an amount"
     :summary     "New transaction"
     :produces    {:media-type "application/json"}
     :methods     {:post
                   {:parameters {:body Transaction}
                    :consumes   "application/json"
                    :response   (fn [ctx]
                                  (let [tx (get-in ctx [:parameters :body])
                                        index (add-transaction! storage tx)]
                                    {:message (format "Transaction will be added to Block %s" index)}))}}}))

(defn chain-resource [{:keys [chain]}]
  (resource
    {:id       ::chain
     :summary  "Get the entire chain"
     :produces {:media-type "application/json"}
     :methods  {:get
                {:response (fn [_]
                             @chain)}}}))

(defn register-node-resource [nodes]
  (resource
    {:id          ::registration
     :summary     "Register a new node"
     :description "Add a new node to the list of nodes. Eg. 'http://192.168.0.5:5000'"
     :produces    {:media-type "application/json"}
     :methods     {:post {:parameters {:body #{NodeAddress}}
                          :consumes "application/json"
                          :response (fn [ctx]
                                      (let [new-nodes (get-in ctx [:parameters :body])]
                                        (add-node! nodes new-nodes)
                                        {:message    "New nodes have been added"
                                         :node-count (-> @nodes :nodes count)}))}}}))

(defn resolve-node-resource [{:keys [chain]} nodes]
  (resource
    {:id ::resolve
     :summary "Finds authoritative chain"
     :methods {:get {:response (fn [_]
                                 ((let [consensus-chain (resolve-conflict chain nodes)]
                                    (replace-chain! chain consensus-chain))))}}}))

(defn create-listener [port storage nodes]
  (listener
    ["/"
     [
      ["mine" (mine-resource storage nodes)]
      ["chain" (chain-resource storage)]
      ["transactions/"
       [
        ["new" (new-transaction-resource storage)]]]
      ["nodes/"
       [
        ["register" (register-node-resource nodes)]
        ["resolve" (resolve-node-resource storage nodes)]]]
      [true (as-resource nil)]]]
    {:port port}))

(defmethod ig/init-key :blockchain-hackernoon.http/server [_ {:keys [port storage nodes]}]
  (create-listener port storage nodes))

(defmethod ig/halt-key! :blockchain-hackernoon.http/server [_ {:keys [listener]}]
  (when-let [close (:close listener)]
    (close)))

