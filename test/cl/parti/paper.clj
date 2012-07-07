(ns cl.parti.analysis-test
  (:use (cl.parti analysis diagonal output utils))
  (:use clojure.test)
  (:use clojure.java.io))

;(def do-lower
;  (partial dump-lower normalize-histogram no-pre-editor rectangle
;    (print-tick 100)))
;
;(deftest test-lower
;  (dopar [n [5 6 7 8 9 10 11 12 13 14 15 20 25 30]]
;    (time (do-lower "/home/andrew/project/particl/data/dump" n "a" 10000000))))

(defn top
  [in out n infix bits n-samples shutdown]
  (let [tick (print-tick 1000)
        best (nearest-in-dump tick in n bits n-samples shutdown 1)]
    (with-open [w (writer out)]
      (doseq [[[a b] m] (take 1000 best)]
        (let [a (str n infix a)
              b (str n infix b)
              diff
              (pair-difference normalize-histogram no-pre-editor rectangle
                infix n a b)]
          (.write w (str a " " b " " m " " diff "\n")))))))

(deftest test-top
  (doseq [n (range 12 16)]
    (let [in (str "/home/andrew/project/particl/data/dump-" n "-a.dmp")
          n-bits (cond (> n 8) 2 (> n 6) 3 :else 4)
          n-samples (min 20 (+ 3 (int (/ (* 20 n n) 100))))
          out (str "/home/andrew/project/particl/data/dump-" n "-a-" n-bits "-" n-samples ".best")
          rpt (if (= n 5) 5 20)]
      (time
        (top in out n "a" n-bits n-samples rpt)))))

(run-tests)
