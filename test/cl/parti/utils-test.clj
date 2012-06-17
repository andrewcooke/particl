(ns cl.parti.utils-test
  (:use cl.parti.utils)
  (:use clojure.test))


(deftest test-unsign-byte
  (is (= 255 (unsign-byte -1)))
  (is (= 254 (unsign-byte -2)))
  (is (= 0 (unsign-byte 0)))
  (is (= 1 (unsign-byte 1)))
  (is (= 127 (unsign-byte 127))))

(deftest test-roundtrip-byte
  (doseq [i (range 256)]
    (is (= i (unsign-byte (sign-byte i)))))
  (doseq [i (range -128 128)]
    (is (= i (sign-byte (unsign-byte i))))))

(deftest test-map-state
  (is (= [[0 1 2] 3]
        (map-state (fn [state x] [state (inc state)]) 0 [:a :b :c]))))

(deftest test-map-state-2
  (is (= [[[0 1] [2 3]] 4]
        (map-state-2 (fn [state x] [state (inc state)]) 0 [[:a :b] [:c :d]]))))

(run-tests)
