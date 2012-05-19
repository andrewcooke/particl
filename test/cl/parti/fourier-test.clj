(ns cl.parti.fourier-test
  (:use (cl.parti fourier))
  (:use clojure.test))


(deftest test-perm
  (is (= [[1 2 3] [1 2 -3] [1 -2 3] [1 -2 -3] [-1 2 3] [-1 2 -3] [-1 -2 3] [-1 -2 -3]] (perm [1 2 3]))))

(run-tests)
