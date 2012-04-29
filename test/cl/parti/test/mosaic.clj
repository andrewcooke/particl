(ns cl.parti.test.mosaic
  (:use (cl.parti hsl mosaic random))
  (:use clojure.test))

(deftest test-mosaic
  (is (= [2 1 1 [[[0 0 1] [0 0 1]] [[0 0 1] [0 0 1]]]] (mosaic 2 1 1 white))))

(deftest test-transform
  (let [m (mosaic 2 1 1 red)
        s (byte-stream (queue [1]))
        m (repeated-transform m 1 (make-colourblind 1) s)]
    ; only check unchanged part
    (is (= ((m 3) 0) [[0. 1. 0.5] [0. 1. 0.5]]))))

(deftest test-assemble-no-border
  (is (= [[:x :x :x :x] [:x :x :x :x] [:x :x :x :x] [:x :x :x :x]]
        (print-mosaic [print-identity identity] (mosaic 2 1 1 :x ) 2 [nil 0])))
  (is (= [[:x :x] [:x :x]]
        (print-mosaic [print-identity identity] (mosaic 1 1 1 :x ) 2 [nil 0])))
  (is (= [[:x]]
        (print-mosaic [print-identity identity] (mosaic 1 1 1 :x ) 1 [nil 0])))
  (is (= [[:y :y :y] [:y :x :y] [:y :y :y]]
        (print-mosaic [print-identity identity] (mosaic 1 1 1 :x ) 1 [:y 1])))
  (is (= [[:y :y :y :y :y] [:y :y :y :y :y] [:y :y :x :y :y] [:y :y :y :y :y] [:y :y :y :y :y]]
        (print-mosaic [print-identity identity] (mosaic 1 1 1 :x ) 1 [:y 2])))
  )

(deftest test-bracket
  (is (= [[1] [2] [1] [3] [1]] (bracket-interpose [1] [[2] [3]]))))

(run-tests)
