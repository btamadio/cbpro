(defproject cbpro "0.1.0-SNAPSHOT"
  :description "coinbase pro client"
  :url "http://github.com/btamadio/cbpro"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [coinbase-pro-clj "1.0.0"]
                 [org.clojure/core.async "1.3.610"]]
  :main ^:skip-aot cbpro.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
