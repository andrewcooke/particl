(ns cl.parti.diagonal-test
  (:use (cl.parti main))
  (:use clojure.test))


(deftest test-rcetangle
  (-main "-r" "rectangle" "-v" "-i" "word" "-o" "/tmp/rectangle-test.png" "abc"))

(deftest test-square
  (-main "-r" "square" "-v" "-i" "word" "-o" "/tmp/square-test.png" "abc"))

(run-tests)
