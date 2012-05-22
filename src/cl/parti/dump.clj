(ns cl.parti.dump
  (:use (cl.parti state mosaic))
  (:import java.io.File)
  (:import java.io.FileOutputStream)
  (:import java.io.FileInputStream)
  (:use clojure.java.io))


; convert [-1 1) to unsiged byte 0-255 then encode that in a signed byte
; (0 -> 0, 1 -> 1, ... 127 -> 127, 128 -> -128, ... 255 -> -1)
; (actual range is [-1 1], but with rounding etc this seems to be best)

(defn- to-unsigned [x]
  (let [x (int (* (inc x) 128))]
    (cond
      (> x 255) 255
      (< x 0) 0
      :else x)))

(defn- to-byte [x]
  (let [x (to-unsigned x)]
    (byte (if (< x 128) x (- x 256)))))

(defn- to-bytes [rows]
  (byte-array (flatten (map-rows to-byte rows))))

(defn- touch [path]
  (let [file (File. path)]
    (.createNewFile file)))

(defn- measure [n path]
  (let [file (File. path)
        bytes (.length file)]
    (/ bytes (* n n))))

(def ^:private TILE-NUMBER 16)

(defn- print-count [i factor]
  (cond
    (= 0 (mod i (* 1000 factor))) (println i)
    (= 0 (mod i (* 100 factor))) (do (print "O") (flush))
    (= 0 (mod i (* 10 factor))) (do (print "o") (flush))
    (= 0 (mod i factor)) (do (print ".") (flush))))

(defn dump [path n render]
  (let [options {:tile-number TILE-NUMBER , :complexity 1}]
    (touch path)
    (let [start (measure TILE-NUMBER path)
          make-state (string-state "SHA-1")]
      (println "skipping" start "existing entries")
      (with-open [out (FileOutputStream. path Boolean/TRUE)]
        (doseq [i (range start (+ start n))]
          (let [state (make-state (str i))
                row-11 (render options state)]
            (print-count i 10)
            (.write out (to-bytes row-11))))))))


; returns nil at eof, otherwise fills buffer
(defn- fill-buffer [stream buffer offset target]
  (if (= offset target)
    buffer
    (let [delta (.read stream buffer offset (- target offset))]
      (if (< delta 1)
        nil
        (recur stream buffer (+ delta offset) target)))))

(defn- map-buffer [stream n f buffer]
  (lazy-seq
    (if-let [buffer (fill-buffer stream buffer 0 n)]
      (cons (f buffer) (map-buffer stream n buffer))
      nil)))

(defn map-dump [path f]
  (let [n (* TILE-NUMBER TILE-NUMBER)
        buffer (byte-array (repeat n (byte 0)))]
    (with-open [in (FileInputStream. path)]
      (map-buffer in n f buffer))))

(defn- reduce-buffer
  ([stream n f acc buffer] (reduce-buffer stream n f acc buffer 0))
  ([stream n f acc buffer count]
    (print-count count 100)
    (if-let [buffer (fill-buffer stream buffer 0 n)]
      (recur stream n f (f acc buffer) buffer (inc count))
      acc)))

(defn reduce-dump [path f zero]
  (let [n (* TILE-NUMBER TILE-NUMBER)
        buffer (byte-array (repeat n (byte 0)))]
    (with-open [in (FileInputStream. path)]
      (reduce-buffer in n f zero buffer))))

; convert from signed byte to unsigned int [0 255]
(defn to-int [x]
  (int
    (if (< x 0)
      (+ 256 x)
      x)))

(defn to-ints [s]
  (map to-int s))

(defn- hist-byte [hist x]
  (let [hist (assoc hist x (inc (get hist x 0)))]
    ;    (println hist)
    hist))

(defn- hist-buffer [hist buffer]
  (reduce hist-byte hist (to-ints buffer)))

(defn hist-dump [path]
  (let [hist (reduce-dump path hist-buffer {})
        biggest (apply max (vals hist))
        scale (/ 60 biggest)]
    (doseq [i (range 0 256)]
      (let [n (get hist i 0)]
        (printf "%3d: %8d %s\n" i n (apply str (repeat (* scale n) "*")))))))
