(ns cl.parti.test.hsl
  (:use (cl.parti hsl))
  (:use clojure.test))

(deftest test-hsl-to-rgb
  (is (= [1 1 1] (rgb white)))
  (is (= [0 0 0] (rgb black)))
  (is (= [1 0 0] (rgb red)))
  (is (= [0 1 0] (rgb green)))
  (is (= [0 0 1] (rgb blue)))
  )

(run-tests)
