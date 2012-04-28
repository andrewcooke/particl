(ns cl.parti.test.png
  (:use clojure.java.io)
  (:use (cl.parti hsl mosaic random png))
  (:use clojure.test))


(deftest test-write
  (let [print-png (make-print-png (output-stream "/tmp/mosaic-test.png"))
        m (mosaic 3 1 red)
        border [black 3]]
    (print-mosaic print-png png-8-bit m 10 border)))

(run-tests)
