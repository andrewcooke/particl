(ns cl.parti.dump-test
  (:use (cl.parti dump diagonal output))
  (:use clojure.test))


;(deftest test-dump-20
;  (time (dump "/tmp/square-20.dmp" 1000000 square 20 ""))
;  (time (dump "/tmp/rectangle-20.dmp" 1000000 rectangle 20 "")))

(deftest test-dump-10
  (time (dump "/tmp/square-10.dmp" 1000000 normalize-histogram square 10 "a"))
  (time (dump "/tmp/rectangle-10.dmp" 1000000 normalize-histogram rectangle 10 "a")))

;(deftest test-hist-dump
;  (time (hist-dump "/tmp/square-20.dmp" 20)))

(defn group
  [path]
  (let [pairs (group-dump path 20 6 [15 20] 1)
        pairs (sort-by (fn [[[a b] n]] (* -1 n)) pairs)]
    (println (count pairs) "matches")
    (doseq [[[a b] n] pairs]
      (println a b n))))

;(deftest test-square-group
;  (group "/tmp/square-20.dmp"))

;(deftest test-rectangle-group
;  (group "/tmp/rectangle-20.dmp"))

(run-tests)
