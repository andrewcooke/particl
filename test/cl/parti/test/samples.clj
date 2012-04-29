(ns cl.parti.test.samples
  (:use clojure.java.io)
  (:use (cl.parti hsl mosaic random png))
  (:use clojure.test))


(deftest test-samples
  (doseq [[n s w bg t] [[5 5 20 [white 3] 20][500 16 4 [black 1] 40]]]
    (doseq [i (range n)]
      (let [path (apply str (interpose "-"
                              ["/tmp/sample" s w i ".png"]))
            state (hash-state path)
            [mosaic state] (random-mosaic s state)
            mosaic (repeated-transform mosaic t (make-colourblind (mosaic 2)) state)
            print (print-png-indexed (output-stream path))]
        (print-mosaic print mosaic w bg)))))

(run-tests)
