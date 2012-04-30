(ns cl.parti.png-test
  (:use clojure.java.io)
  (:use (cl.parti hsl mosaic random png))
  (:use clojure.test))


(deftest test-write-8-bit
  (let [print (print-png-8-bit (output-stream "/tmp/mosaic-test-8-bit.png"))
        m (mosaic 3 1 1 red)
        border [black 3]]
    (print-mosaic print m 10 border)))

(deftest test-write-indexed
  (let [print (print-png-indexed (output-stream "/tmp/mosaic-test-indexed.png"))
        m (mosaic 3 1 1 red)
        border [black 3]]
    (print-mosaic print m 10 border)))

(run-tests)
