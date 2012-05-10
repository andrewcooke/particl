(ns cl.parti.random-test
  (:use clojure.test)
  (:use (cl.parti random))
  (:use clojure.math.numeric-tower))


; expect a given byte to be repeated approx every 256 times,
; with an sd of 16.

(defn lengths
  ([r] (lengths (rest r) (first r) 1))
  ([r a n]
    (let [b (first r)]
      (if (= a b)
        (lazy-seq (cons n (lengths (rest r) a 1)))
        (recur (rest r) a (inc n))))))

(deftest test-lengths
  (is (= [1 3 2] (take 3 (lengths [0 0 1 1 0 1 0 0])))))

(defn sqr [x] (* x x))

(defn assert-stats [s mean sd err]
  (let [n (count s)
        s-mean (/ (apply + s) n)
        s-sd (sqrt (/ (apply + (map #(sqr (- % s-mean)) s)) n))]
    (println "mean" (double s-mean) "sd" (double s-sd))
    (is (< s-mean (* (+ 1 err) mean)))
    (is (> s-mean (* (- 1 err) mean)))
    (is (< s-sd (* (+ 1 err) sd)))
    (is (> s-sd (* (- 1 err) sd)))))

(println (take 32 (random (byte-array 64 (byte 0)))))

(deftest measure-repeat
  (let [r (random (byte-array 1 (byte 1)))
        l (lengths r)]
    (assert-stats (take 1000 l) 256 256 0.1))
  (let [r (random (byte-array 64 (byte 1)))
        l (lengths r)]
    (assert-stats (take 1000 l) 256 256 0.1)))

(defn filter-dups [s values]
  (if-let [values (seq values)]
    (let [[n i v] (first values)]
      (if (s v)
        (do
          (println "duplicate!" n i v)
          (recur s (rest values)))
        (recur (conj s v) (rest values))))
    s))

(deftest check-bit-sensitive
  (let [sizes [1 127 128 129 255 256 257 512]
        values
        (for [n-bits sizes
              index (range n-bits)]
          (let [hash
                (byte-array
                  (for [i (range n-bits)]
                    (byte (if (= i index) 1 2))))]
            [n-bits index (take 4 (random hash))]))
        s (filter-dups #{} values)]
;    (println values)
;    (println s)
    (is (= (apply + sizes) (count s)))))

(run-tests)
