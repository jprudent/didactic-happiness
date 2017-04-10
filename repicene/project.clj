(defproject repicene "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]
                 [org.clojure/core.async "0.3.442"]
                 [http-kit "2.2.0"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}
             :with-assert {:global-vars {*assert* false}}}
  :global-vars {*assert* false}
  :main repicene.launch)
