(ns cl.parti.main-test
  (:use (cl.parti main))
  (:use clojure.test))


(defn stream-consumer [stream]
  (println "read" (count stream) "lines"))

(defn broken-open [file]
  (with-open [rdr (clojure.java.io/reader file)]
    (line-seq rdr)))

(deftest test-open
  (try
    (stream-consumer (broken-open "/etc/passwd"))
    (catch RuntimeException e
      (println "caught " e)))
  (let [stream (lazy-open "/etc/passwd")]
    (println "have stream")
    (stream-consumer stream)))

(run-tests)
