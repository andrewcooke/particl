(ns cl.parti.dump-test
  (:use (cl.parti dump square))
  (:use clojure.test))


(deftest test-dump-square
  (time (dump "/tmp/square.dmp" 1000000 square 20)))

;(deftest test-hist-dump
;  (time (hist-dump "/tmp/square.dmp")))

(run-tests)
