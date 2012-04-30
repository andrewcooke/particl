(ns cl.parti.random-test
  (:use (cl.parti random))
  (:use clojure.test))


(deftest test-state
  (let [s (state "hello world")]
    (is (= (peek s) 48))))

(deftest test-rotate
  (is (= 2 (rotate-byte 1 4)))
  (is (= 1 (rotate-byte 2 4)))
  (is (= -128 (rotate-byte 3 4)))
  (is (= -86 (rotate-byte 1 0x55)))
  (is (= 0x55 (rotate-byte 1 -86))))

(defn index-of [x s]
  (first (first (filter
                  (fn [[i y]] (= x y))
                  (map-indexed (fn [a b] [a b]) s)))))

(deftest test-byte-stream
  (let [b (byte-stream (queue [1 2 3]))]
    (is (= 1 (first b)))
    (is (= [1 2 3 117 21 53 -5 -9 -13 42] (take 10 b))))
  (let [b (byte-stream (queue [1]))
        a (first b)
        b (rest b)]
    (is (= 7 (index-of a b)))))

(run-tests)
