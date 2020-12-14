(ns cbpro.core
  (:require [coinbase-pro-clj.core :as cp]
            [clojure.core.async :refer [go]]))

(defn order-book [product-ids]
  (atom (into {} (map (fn [x]
                        {x {:bids (sorted-map) :asks (sorted-map)}})
                      product-ids))))

(defn order-book-product-ids [order-book]
  (vec (keys order-book)))

(defn to-doubles [v]
  (vec (map #(Double/parseDouble %) v)))

(defn snapshot-bids [snapshot-event]
  (into (sorted-map-by >) (map to-doubles (:bids snapshot-event))))

(defn snapshot-asks [snapshot-event]
  (into (sorted-map) (map to-doubles (:asks snapshot-event))))

(defn handle-snapshot [order-book event]
  (assoc order-book (:product_id event)
         {:bids (snapshot-bids event)
          :asks (snapshot-asks event)}))

(defn apply-change [product-id order-book change]
  (let [side ({"buy" :bids "sell" :asks} (change 0))
        price (Double/parseDouble (change 1))
        size (Double/parseDouble (change 2))]
    (if (zero? size)
      (update-in order-book [product-id side] dissoc price)
      (assoc-in order-book [product-id side price] size))))

(defn apply-changes [order-book product-id changes]
  (reduce (partial apply-change product-id) order-book changes))

(defn update-timestamp [order-book product-id timestamp]
  (assoc-in order-book [product-id :timestamp] timestamp))

(defn handle-l2update [order-book event]
  (-> order-book
      (apply-changes (:product_id event) (:changes event))
      (update-timestamp (:product_id event) (:time event))))

(defn event-handler [event-type]
  (cond
    (= event-type "snapshot") handle-snapshot
    (= event-type "l2update") handle-l2update
    :else (fn [order-book event] order-book)))

(defn update-book [order-book event]
  (let [handler (event-handler (:type event))]
    (handler order-book event)))

(defn start-order-book [order-book]
  (cp/create-websocket-connection {:url cp/websocket-url
                                   :product_ids (order-book-product-ids @order-book)
                                   :channels [{:name "level2"}]
                                   :on-receive #(swap! order-book update-book %)}))

(defn display-book [order-book]
  (doseq [product-id (order-book-product-ids order-book)]
    (println product-id (:timestamp (order-book product-id)))
    (println "\tbid: " (first (:bids (order-book product-id))))
    (println "\task: " (first (:asks (order-book product-id))))))

(defn -main [& args]
  (def ob (order-book (vec args)))
  (go (start-order-book ob))
  (loop []
    (Thread/sleep 1000)
    (display-book @ob)
    (recur)))
