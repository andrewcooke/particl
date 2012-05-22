(ns cl.parti.dump-test
  (:use (cl.parti dump fourier))
  (:use clojure.test))


;(deftest test-dump-fourier
;  (dump "/tmp/fourier.dmp" 9000000 fourier))

(deftest test-hist-dump
  (hist-dump "/tmp/fourier.dmp"))

(run-tests)
