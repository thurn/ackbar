(defproject ackbar "1.0.0-SNAPSHOT"
  :description "IT'S A TRAP!"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.5.3"]
                 [hiccup "0.3.1"]
                 [ring/ring-jetty-adapter "0.3.1"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
                     [appengine-magic "0.3.0-SNAPSHOT"]]
  :namespaces [ackbar.app_servlet])
