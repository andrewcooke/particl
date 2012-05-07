(ns cl.parti.samples-test
  (:use clojure.java.io)
  (:use (cl.parti hsl mosaic state png))
  (:use clojure.test))


(deftest test-samples
  (doseq [[n s w bg k] [[500 5 20 [white 3] 40][500 16 4 [black 1] 64]]]
    (doseq [i (range n)]
      (let [path (apply str (interpose "-"
                              ["/tmp/sample" s w i ".png"]))
            state (hash-string path)
            [mosaic state] (random-mosaic s state)
            mosaic (repeated-transform mosaic k (make-colourblind s (mosaic 2)) state)
            print (print-png-indexed (output-stream path))]
        (print-mosaic print mosaic w bg)))))

(run-tests)
