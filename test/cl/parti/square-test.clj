(ns cl.parti.square-test
  (:use (cl.parti main))
  (:use clojure.test))


(deftest test-square
  (-main "-r" "square" "-v" "-i" "word" "-o" "/tmp/square-test.png" "abc"))

(run-tests)
