(ns cl.parti.dump-test
  (:use (cl.parti dump diagonal))
  (:use clojure.test))


;(deftest test-dump-20
;  (time (dump "/tmp/square-20.dmp" 1000000 square 20))
;  (time (dump "/tmp/rectangle-20.dmp" 1000000 rectangle 20)))

(deftest test-hist-dump
  (time (hist-dump "/tmp/square-20.dmp" 20)))

(run-tests)
