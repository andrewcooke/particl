(ns cl.parti.analysis-test
  (:use (cl.parti analysis diagonal output utils))
  (:use clojure.test)
  (:use clojure.java.io))

;(deftest test-lower
;  (is (= [[0 0]] (lower 1 [0 0])))
;  (is (= [[0 0] [0 1] [1 1]] (lower 2 [0 0])))
;  (is (= [[0 3] [0 4] [1 4]] (lower 2 [0 1])))
;  (is (= [[3 3] [3 4] [4 4]] (lower 2 [1 1]))))
;
;(deftest test-upper
;  (is (= [[3 5] [3 4] [2 4] [3 3] [2 3] [1 3]] (upper 3 [0 0]))))
;
;(deftest test-left
;  (is (= [[0 0] [0 1] [0 2] [1 1]] (left 4)))
;  (is (= [[0 0] [0 1] [0 2] [0 3] [1 1] [1 2]] (left 5))))
;
;(deftest test-bottom
;  (is (= [[1 3] [2 3] [3 3] [2 2]] (bottom 4)))
;  (is (= [[1 4] [2 4] [3 4] [4 4] [2 3] [3 3]] (bottom 5))))

(def do-dump-single
  (partial dump-single normalize-histogram rectangle (print-tick 100)))

(deftest test-dump
;  (println (macroexpand '(dopar [n (range 7 11)]
;    (time (do-dump-single "/tmp/single" "a" n 10000000))))))
  (dopar [n (range 7 11)]
    (time (do-dump-single "/tmp/single" "a" n 10000000))))

;(defn top
;  [in out prefix n bits n-samples startup]
;  (let [best (nearest-in-dump (print-tick 1000) in n bits n-samples startup 1)]
;    (with-open [w (writer out)]
;      (doseq [[[a b] m] best] (.write w (str prefix a " " prefix b " " m "\n"))))))

;(def dump-rectangle
;  (partial dump-single normalize-histogram rectangle (print-tick 100)))
;
;(deftest test-dump-9
;  (time (dump-rectangle "/tmp/a-4.dmp" "a" 4 10000000)))

;(deftest test-top-9
;  (time (top "/tmp/rectangle-9-n.dmp" "/tmp/rectangle-9-n.best" "d" 9 3 12 3)))
;
;(deftest test-dump-8
;  (time (dump (print-tick 100) "/tmp/rectangle-8-n.dmp" 10000000 normalize-histogram rectangle 8 "b")))
;
;(deftest test-top-8
;  (time (top "/tmp/rectangle-8-n.dmp" "/tmp/rectangle-8-n.best" "b" 8 3 12 3)))
;
;(deftest test-dump-7
;  (time (dump (print-tick 100) "/tmp/rectangle-7-n.dmp" 10000000 normalize-histogram rectangle 7 "c")))
;
;(deftest test-top-7
;  (time (top "/tmp/rectangle-7-n.dmp" "/tmp/rectangle-7-n.best" "c" 7 3 12 3)))

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
