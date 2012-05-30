(ns cl.parti.diagonal-test
  (:use (cl.parti main))
  (:use clojure.test))


(deftest test-rcetangle
  (-main "--builder" "rectangle" "-v" "-i" "word" "-o" "/tmp/rectangle-test.png" "abc"))

(deftest test-square
  (-main "--builder" "square" "-v" "-i" "word" "-o" "/tmp/square-test.png" "abc"))

(run-tests)
