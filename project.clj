(defproject cl.parti "1.0.0-SNAPSHOT"
  :description "Towards graphical cryptographic hashes."
  :repositories {"lib" ~(str (.toURI (java.io.File. "repo")))}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/math.numeric-tower, "0.0.1"]
                 [org.clojure/math.combinatorics, "0.0.2"]
                 [ar.com.hjg/pngj "0.90.0"]
                 [org.clojure/tools.cli "0.2.1"]
                 [commons-codec/commons-codec "1.6"]
                 [net.sf.trove4j/trove4j "3.0.2"]
                 ]
  :dev-dependencies [[lein-marginalia "0.7.0"]
                     ]
  :main cl.parti.main)

