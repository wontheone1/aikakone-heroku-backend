(defproject clojure-getting-started "1.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [compojure "1.6.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [environ "1.0.0"]
                 [cheshire "5.6.0"]
                 [clojure.java-time "0.3.2"]
                 [com.taoensso/sente "1.12.0"]
                 [http-kit "2.2.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring-cors "0.1.7"]]
  :min-lein-version "2.0.0"
  :uberjar-name "aikakone-backend.jar"
  :profiles {:production {:env {:production true}}})
