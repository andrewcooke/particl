(defproject cl.parti "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :repositories {"lib" ~(str (.toURI (java.io.File. "repo")))}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ar.com.hjg/pngj "0.90.0"]])
