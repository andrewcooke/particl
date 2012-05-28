(ns ^{:doc "

Experimental code, in development, to try assess the equivalent number of
bits in a graphical hash.

"
      :author "andrew@acooke.org"}
  cl.parti.dump
  (:use (cl.parti square input utils))
  (:import java.io.File)
  (:import java.io.FileOutputStream)
  (:import java.io.FileInputStream)
  (:import java.io.BufferedInputStream)
  (:use clojure.java.io))


(defn- triangle
  ([rows] (triangle rows 1))
  ([rows n]
    (let [rows (seq rows)]
      (if rows
        (cons (take n (first rows)) (triangle (rest rows) (inc n)))
        nil))))

(defn- to-byte [x]
  (sign-byte (int (* x 127))))

(defn- pack [rows]
  (byte-array (map to-byte (flatten (triangle rows)))))

(defn- touch [path]
  (let [file (File. path)]
    (.createNewFile file)))

(defn- measure [path n]
  (let [file (File. path)
        bytes (.length file)]
    (/ (* 2 bytes) (* n (inc n)))))

(defn- print-count [i factor]
  (cond
    (= 0 (mod i (* 1000 factor))) (println i)
    (= 0 (mod i (* 100 factor))) (do (print "O") (flush))
    (= 0 (mod i (* 10 factor))) (do (print "o") (flush))
    (= 0 (mod i factor)) (do (print ".") (flush))))

(defn dump [path count render n]
  (touch path)
  (let [offset (measure path n)
        hash (word-hash "SHA-1")
        render (render n)]
    (println "skipping" offset "existing entries")
    (with-open [out (FileOutputStream. path Boolean/TRUE)]
      (doseq [i (range offset (+ offset count))]
        (let [state (hash (str i))
              [rows state] (render state)]
          (print-count i 10)
          (.write out (pack rows)))))))

;; returns nil at eof, otherwise fills buffer
;(defn- fill-buffer [stream buffer offset target]
;  (if (= offset target)
;    buffer
;    (let [delta (.read stream buffer offset (- target offset))]
;      (if (< delta 1)
;        nil
;        (recur stream buffer (+ delta offset) target)))))
;
;(defn- map-buffer [stream n f buffer]
;  (lazy-seq
;    (if-let [buffer (fill-buffer stream buffer 0 n)]
;      (cons (f buffer) (map-buffer stream n buffer))
;      nil)))
;
;(defn map-dump [path f]
;  (let [n CHUNK-SIZE
;        buffer (byte-array (repeat n (byte 0)))]
;    (with-open [in (FileInputStream. path)]
;      (map-buffer in n f buffer))))
;
;(defn- reduce-buffer
;  ([stream n f acc buffer] (reduce-buffer stream n f acc buffer 0))
;  ([stream n f acc buffer count]
;    (print-count count 100)
;    (if-let [buffer (fill-buffer stream buffer 0 n)]
;      (recur stream n f (f acc buffer) buffer (inc count))
;      acc)))
;
;(defn reduce-dump [path f zero]
;  (let [n CHUNK-SIZE
;        buffer (byte-array (repeat n (byte 0)))]
;    (with-open [in (BufferedInputStream. (FileInputStream. path))]
;      (reduce-buffer in n f zero buffer))))
;
;; convert from signed byte to unsigned int [0 255]
;(defn to-int [x]
;  (int
;    (if (< x 0)
;      (+ 256 x)
;      x)))
;
;(defn to-ints [s]
;  (map to-int s))
;
;(defn- hist-byte [hist x]
;  (let [hist (assoc hist x (inc (get hist x 0)))]
;    ;    (println hist)
;    hist))
;
;(defn- hist-buffer [hist buffer]
;  (reduce hist-byte hist (to-ints buffer)))
;
;(defn hist-dump [path]
;  (let [hist (reduce-dump path hist-buffer {})
;        biggest (apply max (vals hist))
;        scale (/ 60 biggest)]
;    (doseq [i (range 0 256)]
;      (let [n (get hist i 0)]
;        (printf "%3d: %8d %s\n" i n (apply str (repeat (* scale n) "*")))))))
