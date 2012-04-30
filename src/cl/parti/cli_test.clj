(ns cl.parti.cli-test
  (:use cl.parti.cli)
  (:gen-class))


; compile as main for testing cli

(defn -main [& args]
  (println (handle-args args)))
