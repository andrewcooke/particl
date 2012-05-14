(ns cl.parti.main-test
  (:use (cl.parti main))
  (:use clojure.test))


(deftest test-square
  (-main "-v" "-i" "word" "-o" "/tmp/main-test.png" "abc"))

(run-tests)
