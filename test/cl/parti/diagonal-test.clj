(ns cl.parti.diagonal-test
  (:use (cl.parti main diagonal output random))
  (:use clojure.test))

(deftest test-manhattan
  (let [m8 (manhattan 8)
        m7 (manhattan 7)]
    (is (= 3 (m8 [1 1 0 3])))
    (is (= 2 (m8 [1 1 1 3])))
    (is (= 1 (m8 [1 1 2 3])))
    (is (= 0 (m8 [1 1 3 3])))
    (is (= 0 (m8 [1 1 4 3])))
    (is (= 1 (m8 [1 1 5 3])))
    (is (= 2 (m8 [1 1 6 3])))
    (is (= 3 (m8 [1 1 7 3])))
    (is (= 4 (m8 [1 1 7 2])))
    (is (= 5 (m8 [1 1 7 1])))
    (is (= 6 (m8 [1 1 7 0])))
    (is (= 5 (m8 [2 1 6 0])))
    (is (= 5 (m8 [1 2 7 0])))
    (is (= 4 (m8 [2 2 6 0])))
    (is (= 3 (m7 [1 1 0 3])))
    (is (= 2 (m7 [1 1 1 3])))
    (is (= 1 (m7 [1 1 2 3])))
    (is (= 0 (m7 [1 1 3 3])))
    (is (= 1 (m7 [1 1 4 3])))
    (is (= 2 (m7 [1 1 5 3])))
    (is (= 3 (m7 [1 1 6 3])))
    (is (= 4 (m7 [1 1 6 2])))
    (is (= 5 (m7 [1 1 6 1])))
    (is (= 6 (m7 [1 1 6 0])))))

(defn write-raw
  [n path img]
  (let [[norm _] (normalize-histogram [nil img nil])
        [_ hsl _] ((render-floats n 4 [0 0 0] 1 1 1) [norm nil])]
    ((file-display path) [hsl nil])))

;(deftest test-rectangle-coverage
;  (let [n 10
;        img (for [i (range n)] (for [j (range n)] (+ j (* n i))))
;        img2 (vec (for [i (range n)] (vec (for [j (range n)] 0))))
;        img2 (apply-2 inc img2 [8 8])
;        img2 (apply-2 inc img2 [8 7])
;        ]
;    (write-raw n "/tmp/rectangle-a.png" img)
;    (write-raw n "/tmp/rectangle-b.png" (reflect n img))
;    (write-raw n "/tmp/rectangle-c.png" img2)
;    (write-raw n "/tmp/rectangle-d.png" (reflect n img2))
;    ))

(defn- const-max
  [m state]
  [m state])

(defn rand-images
  [count n r state]
  (lazy-seq
    (if (zero? count)
      nil
      (let [[img state]
            (repeated-transform rand-4 const-max (manhattan n) shift-rectangle n r state)]
        (cons img (rand-images (dec count) n r state))))))

;(deftest test-rectangle-random
;  (let [n 8
;        state (random [(byte 0)])
;        images (rand-images 100 n 1 state)]
;    (do (reduce (fn [i img]
;                  (write-raw n (format "/tmp/rectangle-rand-%d.png" i) img)
;                  (write-raw n (format "/tmp/rectangle-rand-%d-r.png" i) (reflect n img))
;                  (inc i))
;          0 images))))

;(deftest test-rectangle-distribution
;  (let [n 8
;        state (random [(byte 0)])
;        images (rand-images 1 n 1000000 state)]
;    (do (reduce (fn [i img]
;                  (write-raw n (format "/tmp/rectangle-dist-%d.png" i) img)
;                  (write-raw n (format "/tmp/rectangle-dist-%d-r2.png" i) (reflect n img))
;                  (inc i))
;          0 images))))

;(deftest test-rectangle
;  (-main "--builder" "rectangle" "-v" "-i" "word" "-o" "/tmp/rectangle-test.png" "abc"))
;
;(deftest test-square
;  (-main "--builder" "square" "-p" "6" "-v" "-i" "word" "-o" "/tmp/square-test.png" "abc"))

(run-tests)
