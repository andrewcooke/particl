(ns ^{:doc "

Functions to help explore the statistical properties of the generated mosaics.

Analysis here focuses on the internal representation, after normalisation.
This contains the essential 'pattern' in the data, without additional
(and quantifiable) variations due to orientation, choice of hue, etc,
which reduces the search space needed to identify collisions.

"
      :author "andrew@acooke.org"}
  cl.parti.analysis
  (:use (cl.parti input utils))
  (:import java.io.File)
  (:import java.io.FileWriter)
  (:import java.io.FileOutputStream)
  (:import java.io.FileInputStream)
  (:import java.io.BufferedInputStream)
  (:import java.util.Random)
  (:import gnu.trove.map.hash.TLongByteHashMap)
  (:use clojure.java.io)
  (:use clojure.math.numeric-tower)
  (:use clojure.math.combinatorics))

;; ## Generate 'database' of patterns
;;
;; A database provides faster access than generating on the fly (it takes
;; about 10 hours to generate a million 20x20 mosaics on my laptop).

;; ### Stream of mosaics

(defn print-tick
  "Print some output to stdout as a task runs."
  [factor]
  (fn [i]
    (cond
      (= 0 (mod i (* 1000 factor))) (println i)
      (= 0 (mod i (* 100 factor))) (do (print "O") (flush))
      (= 0 (mod i (* 10 factor))) (do (print "o") (flush))
      (= 0 (mod i factor)) (do (print ".") (flush)))))

(defn no-tick
  "Generate a dummy tick function, for use when no output is required."
  [i])

(defn- to-byte
  "Convert the normalized render output, which are floats in [-1 1]
  (analysis here assumes histogram-equalized data), to bytes.

  We need to be careful about rounding to integers about zero.  Scaling to
  [0 255] would give more values at [0] since [0 1) is rounded there.  So
  scale to [0 255.99] as an approximation for [0 256).

  Conversion to (native, signed) bytes uses bit-equivalent values."
  [x]
  (sign-byte (int (* (/ 255.99 2) (+ 1 x)))))

(defn- mosaics-from-hash
  [n infix normalize editor builder tick hash offset]
  (lazy-seq
    (cons
      (let [state (hash (str n infix offset))
            [rows state] (-> state builder editor normalize)]
        (tick offset)
        (map-2 to-byte rows))
      (mosaics-from-hash
        n infix normalize editor builder tick hash (inc offset)))))

(defn- mosaic-stream
  [n infix normalize editor builder tick to-stream]
  (let [hash (word-hash "SHA-1")
        builder (builder n)
        editor (editor n)]
    (to-stream n infix normalize editor builder tick hash 0)))

(defn- mosaic
  [n normalize editor builder word]
  (let [hash (word-hash "SHA-1")
        state (hash word)
        builder (builder n)
        editor (editor n)
        [rows state] (-> state builder editor normalize)]
    (map-2 to-byte rows)))

(defn lower
  [n [x y]]
  (let [m (inc n)
        dx (* m x)
        dy (* m y)]
    (flatten-1
      (for [j (range n)]
        (for [i (range (inc j))]
          [(+ dx i) (+ dy j)])))))

(defn upper
  [n [x y]]
  (let [m (inc n)
        dx (+ n (* m x))
        dy (+ -1 (* 2 n) (* m y))]
    (flatten-1
      (for [j (range n)]
        (for [i (range (inc j))]
          [(- dx i) (- dy j)])))))

(defn left
  [n]
  (flatten-1
    (for [i (range (inc (int (/ n 2))))]
      (for [j (range i (dec (- n i)))]
        [i j]))))

(defn bottom
  [n]
  (flatten-1
    (for [j (range (dec n) (dec (- n (int (/ n 2)))) -1)]
      (for [i (range (- n j) (inc j))]
        [i j]))))

(defn make-pick-bytes
  [xys]
  (fn [rows]
    (map #(nth-2 rows %) xys)))

(defn- triangle-size
  "The number of bytes in a triangle of side `n`."
  [n]
  (/ (* n (inc n)) 2))

;; ### Write lower triangle

(defn byte-sink
  [out pickers]
  (fn [rows]
    (doseq [picker pickers]
      (.write out (byte-array (picker rows))))))

(defn dump-lower
  [normalize editor builder tick path n infix count]
  (let [mosaics
        (mosaic-stream n infix normalize editor builder tick mosaics-from-hash)
        path (format "%s-%d-%s.dmp" path n infix)]
    (with-open [out (FileOutputStream. path Boolean/FALSE)]
      (let [sink (byte-sink out [(make-pick-bytes (lower n [0 0]))])]
        (doseq [mosaic (take count mosaics)]
          (sink mosaic))))))

;; ### Differences

(defn difference
  [a b]
  (let [d
        (reduce-2
          (fn [z [a b]] (let [d (- a b)] (+ z (* d d))))
          0 (zip-2 a b))]
    [(Math/sqrt d)]))

(defn fmt-pair-sink
  [out fmt f]
  (fn [[a b]]
    (.write out (apply (partial format fmt) (f a b)))))

(defn dump-difference
  [normalize editor builder tick path infix n count]
  (let [mosaics
        (mosaic-stream n infix normalize editor builder tick mosaics-from-hash)
        path (format "%s-%d-%s.txt" path n infix)]
    (with-open [out (FileWriter. path Boolean/FALSE)]
      (let [sink (fmt-pair-sink out "%f\n" difference)]
        (doseq [pair (take count (partition 2 mosaics))]
          (sink pair))))))

(defn pair-difference
  [normalize editor builder infix n a b]
  (let [a (mosaic n normalize editor builder a)
        b (mosaic n normalize editor builder b)]
    (first (difference a b))))

;; ### Track bit consumption

(defn track-bits
  [state]
  (let [n (atom 0)]
    (letfn [(tracker [state]
              (fn [required]
                (let [[r state] (state required)]
                  (swap! n #(+ required %))
                  [r (tracker state)])))]
      [n (tracker state)])))

(defn- mosaics-and-bits-from-hash
  [n infix normalize editor builder tick hash offset]
  (lazy-seq
    (cons
      (let [[bits state] (track-bits (hash (str n infix offset)))
            [rows state] (-> state builder editor normalize)]
        (tick offset)
        [@bits (map-2 to-byte rows)])
      (mosaics-and-bits-from-hash
        n infix normalize editor builder tick hash (inc offset)))))

(defn difference-bits
  [[na a] [nb b]]
  (let [[d] (difference a b)]
    [d na nb]))

(defn dump-difference-bits
  [normalize editor builder tick path n infix count]
  (let [mosaics
        (mosaic-stream n infix normalize editor builder tick
          mosaics-and-bits-from-hash)
        path (format "%s-%d-%s.txt" path n infix)]
    (with-open [out (FileWriter. path Boolean/FALSE)]
      (let [sink (fmt-pair-sink out "%f %d %d\n" difference-bits)]
        (doseq [pair (take count (partition 2 mosaics))]
          (sink pair))))))


;(defn dump-pair
;  [normalize render tick path prefix n count]
;  (let [mosaics (mosaic-stream n prefix normalize render tick)]
;    (with-open [out-l (FileOutputStream. (str path "-l.dmp") Boolean/FALSE)
;                out-b (FileOutputStream. (str path "-b.dmp") Boolean/FALSE)]
;      (let [sink-l (make-sink out-l [(make-pick-bytes (left n))])
;            sink-b (make-sink out-b [(make-pick-bytes (bottom n))])]
;        (doseq [mosaic (take count mosaics)]
;          (sink-l mosaic)
;          (sink-b mosaic))))))
;
;(defn- full-triangle
;  "Extract the unique data from the lower triangle of the mosaic."
;  ([label rows] (full-triangle [label []] rows 1))
;  ([[label acc] rows n]
;    (let [rows (seq rows)]
;      (if rows
;        (conj acc (take n (first rows)) (full-triangle (rest rows) (inc n)))
;        [label acc]))))
;
;
;(defn- extract
;  "Take the output from a render function, extract the lower triangle, and
;  scale to one byte per pixel."
;  [rows]
;  (byte-array (map to-byte (flatten (full-triangle rows)))))
;
;(defn- touch
;  "Ensure that file `path` exists."
;  [path]
;  (let [file (File. path)]
;    (.createNewFile file)))
;

;(defn dump
;  "Append `count` patterns to the database file.
;
;  The patterns are based on the SHA-1 hash of the UTF8 encoded string
;  representation of the pattern number, plus the prefix, starting at 0."
;  [tick path count normalize render n prefix]
;  (touch path)
;  (let [offset (measure path n)
;        hash (word-hash "SHA-1")
;        render (render n)]
;    (println "skipping" offset "existing entries")
;    (with-open [out (FileOutputStream. path Boolean/TRUE)]
;      (doseq [i (range offset (+ offset count))]
;        (let [state (hash (str prefix i))
;              [rows state] (normalize (render state))]
;          (tick i)
;          (.write out (extract rows)))))))

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
  ([tick f acc stream buffer] (reduce-buffer tick f acc stream buffer 0))
  ([tick f acc stream buffer count]
    (tick count)
    (if-let [buffer (fill-buffer stream buffer)]
      (recur tick f (f acc buffer) stream buffer (inc count))
      acc)))

(defn- reduce-dump
  "Reduce the database using the function `f`."
  [tick f zero path n]
  (let [buffer (byte-array (repeat (triangle-size n) (byte 0)))]
    (with-open [in (BufferedInputStream. (FileInputStream. path))]
      (reduce-buffer tick f zero in buffer))))

(defn- map-buffer
  "Divide the stream into `buffer`-size chunks and map the function over
  these."
  ([tick f stream buffer] (map-buffer f stream buffer 0))
  ([tick f stream buffer count]
    (lazy-seq
      (tick count)
      (if-let [buffer (fill-buffer stream buffer)]
        (cons (f buffer) (map-buffer tick f stream buffer (inc count))
          nil)))))

(defn- map-dump
  "Map over the database using the function `f`."
  [tick f path n]
  (let [buffer (byte-array (repeat (triangle-size n) (byte 0)))]
    (with-open [in (BufferedInputStream. (FileInputStream. path))]
      (map-buffer tick f in buffer))))

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
  [tick path n]
  (let [cs (triangle-size n)
        hist (reduce-dump tick hist-buffer {} path cs)
        biggest (apply max (vals hist))
        scale (/ 60 biggest)]
    (println)
    (println path)
    (doseq [i (range 256)]
      (let [n (get hist i 0)]
        (printf "%3d: %8d %s\n" i n (apply str (repeat (* scale n) "*")))))))

;; ## Identify 'close' patterns
;;
;; Locality-sensitive hashing identifies 'neighbours; pairs of neighbours
;; are recorded; the process repeats until a pair is identified by a
;; sufficient number of hashes.

;; #### Sampling the pattern to generate a locality-sensitive hash.

(defn- select-samples
  "Generate indices into the pattern for the samples.

A `java.util.Random` is used to provide a repeatable sequence."
  ([n random from] (select-samples n random from []))
  ([n random from samples]
    (if (zero? n)
      samples
      (let [sample (nth from (.nextInt random (count from)))]
        (recur (dec n) random
          (filterv #(not= % sample) from)
          (conj samples sample))))))

(defn- make-mask
  "Given the number of bits to keep, discard the least significant bits."
  ([ones] (make-mask (dec ones) 1 7))
  ([ones mask all]
    (if (zero? all)
      (do (println "mask" mask) mask)
      (let [mask (* 2 mask)]
        (recur (dec ones) (if (> ones 0) (inc mask) mask) (dec all))))))

(defn- select-hash
  "Loop over all samples, sampling the pattern, and masking the values.
Coallesce the bits used into a single long hash."
  ([bits samples mask buffer] (select-hash bits samples mask buffer 0))
  ([bits samples mask buffer hash]
    (let [samples (seq samples)]
      (if samples
        (recur bits (rest samples) mask buffer
          (bit-or (bit-shift-left hash bits)
            (bit-shift-right
              (bit-and mask (nth buffer (first samples))) (- 8 bits))))
        hash))))

(defn- make-hash
  "Construct a function that will be 'reduced' over the database, accumulating
 the hashes for all images in a single pass (with a single selection).

 The output here is a map from hash to all image indices with that hash
 (all 'neighbours')"
  [n bits n-samples path random]
  (assert (<= (* bits n-samples) 64))
  (let [mask (make-mask bits)
        from (vec (range (triangle-size n)))
        samples (select-samples n-samples random from)]
    (println "hash of" bits "bits with" n-samples "samples:" samples)
    (fn [[i matches] buffer]
      (let [hash (select-hash bits samples mask buffer)
            matches (assoc matches hash (conj (get matches hash []) i))
            i (inc i)]
        [i matches]))))

;; #### Counting each image pair.

(defn- pack
  "Reduce a pair of image indices to a single long value.  This saves space
and allows the use of a primitive types collection to map from pairs to
the number of times they are associated."
  [size n1 n2]
  (+ (* size n1) n2))

(defn- unpack
  "Extract the two image indices from the packed value."
  [size pair]
  [(int (/ pair size)) (mod pair size)])

(defn- collect
  "Iterate over the neighbour information, expanding the neighbours into
pairs, and incrementing the count for each pair.

Initially all pairs are added to the map, but once 5 million pairs are
known only existing pairs are incremented.

The map from pair to count is a mutable, primitive type map for
(memory and cpu) efficiency."
  [size matches iter [_ hashes]]
  (println (count hashes) "sets of neighbours")
  (doseq [nbrs (map (comp int-array sort) (vals hashes))]
    (let [n (count nbrs)]
      (when (> n 1)
        (when (> n 100) (println n))
        (doseq [i (range 1 (count nbrs))]
          (when (and (> n 1000) (zero? (mod i 100))) (println " ", i))
          (doseq [j (range i)]
            (let [pair (pack size (nth nbrs i) (nth nbrs j))]
              (let [value (.get matches pair)]
                (if (zero? value)
                  (when (< (.size matches) 5000000) (.put matches pair (byte 1)))
                  (.put matches pair (byte (inc value)))))))))))
  matches)

;; #### Tying everything together.

(defn rev-compare
  "Reverse `compare` to sort in descending order."
  [a b]
  (* -1 (compare a b)))

(defn- complete-many
  [size [iter shutdown] matches]
  (println "iteration" iter "with" (.size matches) "matches")
  (let [candidates (sort rev-compare (.values matches))
        best (first candidates)]
    (println "top match at" best)
    (if (< iter shutdown)
      (do (println "too few iterations") nil)
      (reverse
        (sort-by second
          (map
            (fn [key] [(unpack size key) (.get matches key)])
            (filter (fn [key] (> (.get matches key) (* 0.1 best)))
              (.keys matches))))))))

(defn- measure
  "Measure the size size of a file, in terms of the number of mosaics."
  [path n]
  (/ (.length (File. path)) (triangle-size n)))

(defn nearest-in-dump
  ([tick path n bits n-samples shutdown seed]
    (let [size (measure path n)]
      (nearest-in-dump tick path n bits n-samples size [0 shutdown]
        (Random. seed) (TLongByteHashMap.))))
  ([tick path n bits n-samples size [iter shutdown] random matches]
    (if-let [results (complete-many size [iter shutdown] matches)]
      results
      (recur tick path n bits n-samples size [(inc iter) shutdown] random
        (collect size matches iter
          (reduce-dump
            tick
            (make-hash n bits n-samples path random)
            [0 {}] path n))))))

