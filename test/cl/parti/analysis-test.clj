(ns cl.parti.analysis-test
  (:use (cl.parti analysis diagonal output))
  (:use clojure.test)
  (:use clojure.java.io))


(deftest test-dump-8
  (time (dump (print-tick 100) "/tmp/rectangle-8.dmp" 10000000 normalize-histogram rectangle 8 "b")))

(deftest test-dump-7
  (time (dump (print-tick 100) "/tmp/rectangle-7.dmp" 10000000 normalize-histogram rectangle 7 "c")))

(defn top
  [in out prefix n bits n-samples startup]
  (let [best (nearest-in-dump (print-tick 1000) in n bits n-samples startup 1)]
    (with-open [w (writer out)]
      (doseq [[[a b] m] best] (.write w (str prefix a " " prefix b " " m "\n"))))))

(deftest test-top-7
  (time (top "/tmp/rectangle-7.dmp" "/tmp/rectangle-7.best" "c" 7 3 12 3)))

(deftest test-top-8
  (time (top "/tmp/rectangle-8.dmp" "/tmp/rectangle-8.best" "b" 8 3 12 3)))

;(deftest test-dump-20
;  (time (dump "/tmp/square-20.dmp" 1000000 square 20 ""))
;  (time (dump "/tmp/rectangle-20.dmp" 1000000 rectangle 20 "")))

;(deftest test-dump-10
;  (time (dump "/tmp/square-10.dmp" 1000000 normalize-histogram square 10 "a"))
;  (time (dump "/tmp/rectangle-10.dmp" 1000000 normalize-histogram rectangle 10 "a")))

;(defn group
;  [path n bits]
;  (let [pairs (group-dump (print-tick 1000) path n bits [15 20] 1)
;        pairs (sort-by (fn [[[a b] n]] (* -1 n)) pairs)]
;    (println (count pairs) "matches")
;    (doseq [[[a b] n] pairs]
;      (println a b n))))

;(deftest test-square-10-group
;  (group "/tmp/square-10.dmp" 10))


;(defn group
;  [path]
;  (let [pairs (group-dump path 20 6 [15 20] 1)
;        pairs (sort-by (fn [[[a b] n]] (* -1 n)) pairs)]
;    (println (count pairs) "matches")
;    (doseq [[[a b] n] pairs]
;      (println a b n))))

;(deftest test-hist-dump
;  (time (hist-dump "/tmp/square-20.dmp" 20)))

;(deftest test-square-group
;  (group "/tmp/square-20.dmp"))

;(deftest test-rectangle-group
;  (group "/tmp/rectangle-20.dmp"))

(run-tests)
