(defproject zendesk-open-ticket-checker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.cli "0.3.4"]
                 [clj-http "2.1.0"]
                 [cheshire "5.6.1"]
                 [clj-time "0.11.0"]]
  :main ^:skip-aot zendesk-open-ticket-checker.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :uberjar-name "check-tickets.jar"}})
