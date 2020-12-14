(ns cbpro.core-test
  (:require [clojure.test :refer :all]
            [cbpro.core :refer :all]))

(deftest test-order-book
  (testing "no product ids"
    (let [ob @(order-book [])]
      (is (= {} ob))))
  (testing "one product id"
    (let [ob @(order-book ["BTC-USD"])]
      (is (= {"BTC-USD" {:bids (sorted-map) :asks (sorted-map)}}))))
  (testing "two product ids"
    (let [ob @(order-book ["BTC-USD" "ETH-USD"])]
      (is (= {"BTC-USD" {:bids (sorted-map) :asks (sorted-map)}
              "ETH-USD" {:bids (sorted-map) :asks (sorted-map)}})))))

(deftest test-handle-snapshot
  (let [ob @(order-book ["BTC-USD" "ETH-USD"])
        event {:product_id "BTC-USD"
               :bids [["10101.10" "0.45054140"] ["10100.90" "1.4"]]
               :asks [["10102.55" "0.57753524"] ["10104.47" "0.123"]]}
        actual (handle-snapshot ob event)
        expected {"BTC-USD" {:bids {10101.10 0.45054140 10100.90 1.4}
                             :asks {10102.55 0.57753524 10104.47 0.123}}
                  "ETH-USD" {:bids (sorted-map) :asks (sorted-map)}}]
    (is (= actual expected))))

(deftest test-apply-change
  (let [ob {"BTC-USD" {:bids {10101.10 0.45054140 10100.90 1.4}
                       :asks {10102.55 0.57753524 10104.47 0.123}}}]
    (testing "test buy side low bid"
      (let [actual (apply-change "BTC-USD" ob ["buy" "10099.8" "0.25"])
            expected {"BTC-USD"
                      {:bids {10101.10 0.45054140 10100.90 1.4 10099.8 0.25}
                       :asks {10102.55 0.57753524 10104.47 0.123}}}]
        (is (= actual expected))))
    (testing "test buy side high bid"
      (let [actual (apply-change "BTC-USD" ob ["buy" "10101.25" "1"])
            expected {"BTC-USD"
                      {:bids {10101.25 1.0 10101.10 0.45054140 10100.90 1.4}
                       :asks {10102.55 0.57753524 10104.47 0.123}}}]
        (is (= actual expected))))
    (testing "test buy side zero"
      (let [actual (apply-change "BTC-USD" ob ["buy" "10101.10" "0"])
            expected {"BTC-USD"
                      {:bids {10100.90 1.4}
                       :asks {10102.55 0.57753524 10104.47 0.123}}}]
        (is (= actual expected))))
    (testing "test sell side high ask"
      (let [actual (apply-change "BTC-USD" ob ["sell" "10105.5" "2"])
            expected {"BTC-USD"
                      {:bids {10101.10 0.45054140 10100.90 1.4}
                       :asks {10102.55 0.57753524 10104.47 0.123 10105.5 2.0}}}]
        (is (= actual expected))))    
    (testing "test sell side low ask"
      (let [actual (apply-change "BTC-USD" ob ["sell" "10102.1" "0.1"])
            expected {"BTC-USD"
                      {:bids {10101.10 0.45054140 10100.90 1.4}
                       :asks {10102.1 0.1 10102.55 0.57753524 10104.47 0.123}}}]
        (is (= actual expected))))
    (testing "test sell side zero"
      (let [actual (apply-change "BTC-USD" ob ["sell" "10102.55" "0"])
            expected {"BTC-USD"
                      {:bids {10101.10 0.45054140 10100.90 1.4}
                       :asks {10104.47 0.123}}}]
        (is (= actual expected))))))

(deftest test-handle-l2update
  (let [ob {"BTC-USD" {:bids {10101.10 0.45054140 10100.90 1.4}
                       :asks {10102.55 0.57753524 10104.47 0.123}}}
        event {:product_id "BTC-USD"
               :time "2019-08-14T20:42:27.265Z"
               :changes [["buy" "10100.75" "1.1"]
                         ["sell" "10102.55" "0.0"]
                         ["sell" "10105.1" "1.5"]]}
        actual (handle-l2update ob event)
        expected {"BTC-USD" {:bids {10101.10 0.45054140 10100.90 1.4 10100.75 1.1}
                             :asks {10104.47 0.123 10105.1 1.5}
                             :timestamp "2019-08-14T20:42:27.265Z"}}]
    (is (= actual expected))))
