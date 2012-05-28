(ns ^{:doc "

Experimental code, in development, to try assess the equivalent number of
bits in a graphical hash.

"
      :author "andrew@acooke.org"}
  cl.parti.dump
  (:use (cl.parti input utils))
  (:import java.io.File)
  (:import java.io.FileOutputStream)
  (:import java.io.FileInputStream)
  (:import java.io.BufferedInputStream)
  (:use clojure.java.io))

;; ## Generate 'database' of patterns
;;
;; Here we generate a reasonably compact version of the output from render
;; functions.  The generated files are used later to find collisions.

(defn- triangle
  "Extract the unique data from the lower triangle of the pattern."
  ([rows] (triangle rows 1))
  ([rows n]
    (let [rows (seq rows)]
      (if rows
        (cons (take n (first rows)) (triangle (rest rows) (inc n)))
        nil))))

(defn- to-byte
  "Convert the render output, which are floats in (-1 1), to bytes.

  We need to be careful about rounding to integers about zero.  Shifting to
  to (0 2) then scaling to (0 256) gives a result that rounds down to [0 255],
  keeping the full range of data with uniform intervals.

  Conversion to (native, signed) bytes uses bit-equivalent values."
  [x]
  (sign-byte (int (* 128 (+ 1 x)))))

(defn- pack
  "Take the output from a render function, extract the lower triangle, and
  scale to one byte per pixel."
  [rows]
  (byte-array (map to-byte (flatten (triangle rows)))))

(defn- touch
  "Ensure that file `path` exists."
  [path]
  (let [file (File. path)]
    (.createNewFile file)))

(defn- chunk-size
  [n]
  (/ (* n (inc n)) 2))

(defn- measure
  "Measure the size size of a file, in terms of the number of mosaics."
  [path n]
  (* (.length (File. path)) (chunk-size n)))

(defn- print-count
  "Print some output to stdout as a task runs."
  [i factor]
  (cond
    (= 0 (mod i (* 1000 factor))) (println i)
    (= 0 (mod i (* 100 factor))) (do (print "O") (flush))
    (= 0 (mod i (* 10 factor))) (do (print "o") (flush))
    (= 0 (mod i factor)) (do (print ".") (flush))))

(defn dump
  "Append `count` patterns to the database file.

  The patterns are based on the SHA-1 hash of the UTF8 encoded string
  representation of the pattern number, starting at 0."
  [path count render n]
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

;; ## Read and process database contents

(defn- fill-buffer
  "Read `target` bytes into the buffer."
  ([stream buffer] (fill-buffer stream buffer (count buffer) 0))
  ([stream buffer target offset]
  (if (= offset target)
    buffer
    (let [delta (.read stream buffer offset (- target offset))]
      (if (< delta 1)
        nil
        (recur stream buffer target (+ delta offset)))))))

(defn- reduce-buffer
  "Divide the stream into `buffer`-size chunks and fold the function over
  these."
  ([stream f acc buffer] (reduce-buffer stream f acc buffer 0))
  ([stream f acc buffer count]
    (print-count count 100)
    (if-let [buffer (fill-buffer stream buffer)]
      (recur stream f (f acc buffer) buffer (inc count))
      acc)))

(defn- reduce-dump
  "Reduce the database using the function `f`."
  [path n f zero]
  (let [buffer (byte-array (repeat (chunk-size n) (byte 0)))]
    (with-open [in (BufferedInputStream. (FileInputStream. path))]
      (reduce-buffer in f zero buffer))))

;; ## Histogram the pixel values

(defn- to-ints
  "Convert a buffer of (native, signed) bytes to unsigned, bit-equivalent
  integers."
  [s]
  (map unsign-byte s))

(defn- hist-byte
  "Add a value to a histogram of values."
  [hist x]
  (let [hist (assoc hist x (inc (get hist x 0)))]
    hist))

(defn- hist-buffer
  "Add all values from a buffer to a histogram of values."
  [hist buffer]
  (reduce hist-byte hist (to-ints buffer)))

(defn hist-dump
  "Generate a histogram of the values (as unsigned bytes) in the database."
  [path n]
  (let [cs (chunk-size n)
        hist (reduce-dump path cs hist-buffer {})
        biggest (apply max (vals hist))
        scale (/ 60 biggest)]
    (println)
    (println path)
    (doseq [i (range 256)]
      (let [n (get hist i 0)]
        (printf "%3d: %8d %s\n" i n (apply str (repeat (* scale n) "*")))))))

;; ## Identify 'close' patterns

