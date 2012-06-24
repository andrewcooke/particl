(ns cl.parti.fourier-test
  (:use cl.parti.main)
  (:use clojure.test))


(deftest test-fourier
  (-main "-v" "--builder" "fourier" "-i" "word" "-o" "/tmp/fourier-test.png" "ab"))

(run-tests)
