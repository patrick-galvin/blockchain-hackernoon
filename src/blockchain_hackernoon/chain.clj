(ns blockchain-hackernoon.chain
  (:require [digest :refer [sha-256]]
            [schema.core :as s]
            [integrant.core :as ig]
            [clojure.set :refer [union]]
            [clj-http.client :as client])
  (:import (java.util UUID)))

(s/defrecord Transaction
  [sender :- UUID
   recipient :- UUID
   amount :- Number])

(s/defrecord Block
  [index :- Long
   timestamp :- Long
   transactions :- [Transaction]
   proof :- Long
   previous-block-hash :- UUID])

(defn hash-block [{:keys [index timestamp transactions proof previous-block-hash]}]
  (sha-256 (str index timestamp transactions proof previous-block-hash)))

(defn- last-block [chain]
  (-> @chain last))

(defn- new-block! [chain transactions proof]
  (let [{:keys [index] :as lb} (last-block chain)
        hashed (hash-block lb)
        now (System/currentTimeMillis)
        new-block (->Block (inc index) now transactions proof hashed)]
    (alter chain conj new-block)
    new-block))

(defn validate-proof? [last-proof proof]
  (let [ans (-> (str last-proof proof) str sha-256)]
    (= "0000" (subs ans 0 4))))

(defn proof-of-work [last-proof]
  (first (filter #(validate-proof? last-proof %) (iterate inc 0))))

(defn add-node! [nodes urls]
  (swap! nodes update-in [:nodes] union urls))

(defn valid? [previous-block current-block]
  (let [{previous-proof :proof} previous-block
        {current-previous-block-hash :previous-block-hash current-proof :proof} current-block]
    (and (= (hash-block previous-block) current-previous-block-hash)
         (validate-proof? previous-proof current-proof))))

(defn validate-chain2 [chain]
  (let [[_ & remaining] chain]
    (for [[b1,b2] (zipmap chain remaining)
          :let [v? (valid? b1 b2)]]
      (if (false? v?)
        false))
    true))

(defn mine! [{:keys [chain transactions]} {:keys [identifier]}]
  (dosync
    (let [{:keys [proof]} (last-block chain)
          proof-solved (proof-of-work proof)
          txs (conj @transactions (->Transaction identifier identifier 1))]
      (ref-set transactions [])
      (new-block! chain txs proof-solved))))

(defn add-transaction! [{:keys [chain transactions]} transaction]
  (dosync
    (alter transactions conj transaction)
    (-> chain last-block :index)))

(defn replace-chain! [{:keys [chain]} consensus-chain]
  (dosync
    (ref-set chain consensus-chain)))

(defn resolve-conflict [chain nodes]
  (loop [best-length (count chain)
         best-chain chain
         remote-nodes nodes]
    (if (empty? remote-nodes)
      best-chain)
    (let [{:keys [length chain]} (-> (client/get (str (first remote-nodes) "/" chain) {:as :json}) :body)]
      (if (and (> length best-length) (validate-chain2 chain))
        (recur length chain (rest nodes))
        (recur best-length best-chain (rest nodes))))))