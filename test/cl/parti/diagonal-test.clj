(ns cl.parti.diagonal-test
  (:use (cl.parti main diagonal output random))
  (:use clojure.test))


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

(defn- const-one
  [state]
  [1 state])

(defn rand-images
  [count n r state]
  (lazy-seq
    (if (zero? count)
      nil
      (let [[img state]
            (repeated-transform rand-4 const-one shift-rectangle n r state)]
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

(deftest test-rectangle-distribution
  (let [n 8
        state (random [(byte 0)])
        images (rand-images 1 n 1000000 state)]
    (do (reduce (fn [i img]
                  (write-raw n (format "/tmp/rectangle-dist-%d.png" i) img)
                  (write-raw n (format "/tmp/rectangle-dist-%d-r2.png" i) (reflect n img))
                  (inc i))
          0 images))))

;(deftest test-rectangle
;  (-main "--builder" "rectangle" "-v" "-i" "word" "-o" "/tmp/rectangle-test.png" "abc"))
;
;(deftest test-square
;  (-main "--builder" "square" "-p" "6" "-v" "-i" "word" "-o" "/tmp/square-test.png" "abc"))

(run-tests)
