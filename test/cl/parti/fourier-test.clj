(ns cl.parti.fourier-test
  (:use cl.parti.main)
  (:use clojure.test))


(deftest test-fourier
  (-main "-v" "-r" "fourier" "-i" "word" "-o" "/tmp/fourier-test.png" "abc"))

(run-tests)
