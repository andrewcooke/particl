(ns ^{:doc "

Functions to help explore the statistical properties of the generated mosaics.

Analysis here focuses on the internal representation, after normalisation.
This contains the essential 'pattern' in the data, without additional
(and quantifiable) variations due to orientation, choice of hue, etc,
which reduces the search space needed to identify collisions.

"
      :author "andrew@acooke.org"}
  cl.parti.analysis
  cl.parti.dump
  (:use (cl.parti input utils))
  (:import java.io.File)
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

(defn- triangle
  "Extract the unique data from the lower triangle of the mosaic."
  ([rows] (triangle rows 1))
  ([rows n]
    (let [rows (seq rows)]
      (if rows
        (cons (take n (first rows)) (triangle (rest rows) (inc n)))
        nil))))

(defn- triangle-size
  "The number of bytes in the lower triangle "
  [n]
  (/ (* n (inc n)) 2))

(defn- to-byte
  "Convert the normalized render output, which are floats in [-1 1], to bytes.

  We need to be careful about rounding to integers about zero.  Scaling to
  [0 255] would give more values at [0] since [0 1) is rounded there.  So
  scale to [0 255.99] as an approximation for [0 256).

  Conversion to (native, signed) bytes uses bit-equivalent values."
  [x]
  (sign-byte (int (* (/ 255.99 2) (+ 1 x)))))

(defn- extract
  "Take the output from a render function, extract the lower triangle, and
  scale to one byte per pixel."
  [rows]
  (byte-array (map to-byte (flatten (triangle rows)))))

(defn- touch
  "Ensure that file `path` exists."
  [path]
  (let [file (File. path)]
    (.createNewFile file)))

(defn- measure
  "Measure the size size of a file, in terms of the number of mosaics."
  [path n]
  (/ (.length (File. path)) (triangle-size n)))

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
  (fn [i]))

(defn dump
  "Append `count` patterns to the database file.

  The patterns are based on the SHA-1 hash of the UTF8 encoded string
  representation of the pattern number, plus the prefix, starting at 0."
  [tick path count normalize render n prefix]
  (touch path)
  (let [offset (measure path n)
        hash (word-hash "SHA-1")
        render (render n)]
    (println "skipping" offset "existing entries")
    (with-open [out (FileOutputStream. path Boolean/TRUE)]
      (doseq [i (range offset (+ offset count))]
        (let [state (hash (str prefix i))
              [rows state] (normalize (render state))]
          (tick i)
          (.write out (extract rows)))))))

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
  ([f acc stream buffer] (reduce-buffer f acc stream buffer 0))
  ([f acc stream buffer count]
    (print-count count 100)
    (if-let [buffer (fill-buffer stream buffer)]
      (recur f (f acc buffer) stream buffer (inc count))
      acc)))

(defn- reduce-dump
  "Reduce the database using the function `f`."
  [f zero path n]
  (let [buffer (byte-array (repeat (triangle-size n) (byte 0)))]
    (with-open [in (BufferedInputStream. (FileInputStream. path))]
      (reduce-buffer f zero in buffer))))

(defn- map-buffer
  "Divide the stream into `buffer`-size chunks and map the function over
  these."
  ([f stream buffer] (map-buffer f stream buffer 0))
  ([f stream buffer count]
    (lazy-seq
      (print-count count 100)
      (if-let [buffer (fill-buffer stream buffer)]
        (cons (f buffer) (map-buffer f stream buffer (inc count))
          nil)))))

(defn- map-dump
  "Map over the database using the function `f`."
  [f path n]
  (let [buffer (byte-array (repeat (triangle-size n) (byte 0)))]
    (with-open [in (BufferedInputStream. (FileInputStream. path))]
      (map-buffer f in buffer))))

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
  (let [cs (triangle-size n)
        hist (reduce-dump hist-buffer {} path cs)
        biggest (apply max (vals hist))
        scale (/ 60 biggest)]
    (println)
    (println path)
    (doseq [i (range 256)]
      (let [n (get hist i 0)]
        (printf "%3d: %8d %s\n" i n (apply str (repeat (* scale n) "*")))))))

;; ## Identify 'close' patterns
;;
;; Locality-sentitive hashing identifies 'neighbours; pairs of neighbours
;; are recorded; the process repeats until a pair is identified by a
;; sufficient number of hashes.

;; #### Sampling the pattern to generate a locality-sensitive hash.

(def ^:private
  ^{:doc "Scaling factor for the calculation below."}
    EXPECTED_N 1e-4)

(defn- estimate-samples
  "Set the number of samples so that the expected number of pairs in the
   database, for perfectly random mosaics, is N.  The low value of N needed
   (above) reflects the regularity of the patterns, I think (even so, it
   is surprising - perhaps there is an error here?)."
; p^n-samples * size = N
; p^n-samples = N/size
; n-samples * log(p) = log(N) - log(size)
; n-samples = (log(N) - log(size))/log(p)
; p = 1 / max
; n-samples = log(size/N)/log(max)
  [n bits size]
  (let [n-samples (int (/ (Math/log (/ size EXPECTED_N)) (Math/log (bit-shift-left 1 bits))))]
    (println n-samples "samples")
    n-samples))

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
  "Sample the pattern and mask the values."
  ([samples mask buffer] (select-hash samples mask buffer []))
  ([samples mask buffer hash]
    (let [samples (seq samples)]
      (if samples
        (recur (rest samples) mask buffer
          (conj hash (bit-and mask (nth buffer (first samples)))))
        hash))))

(defn- make-hash
  "Construct a function that will be 'reduced' over the database, accumulating
  the hashes for a single pass.

  The output here is a map from hash to all image indices with that hash
  (all 'neighbours')"
  [n bits path random]
  (let [mask (make-mask bits)
        size (measure path n)
        n-samples (estimate-samples n bits size)
        samples (select-samples n-samples random (range n))]
    (println "hash of" bits "bits with" n-samples "samples:" samples)
    (fn [[i matches] buffer]
      (let [hash (select-hash samples mask buffer)
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

  The map from pairs to count is a mutable, primitive type map for
  (memory and cpu) efficiency."
  [size matches [_ hashes]]
  (println (count hashes) "sets of neighbours")
  (doseq [nbrs (map (comp int-array sort) (vals hashes))]
    (let [n (count nbrs)]
      (when (> n 1)
        (when (> n 100) (println n))
        (doseq [i (range 1 (count nbrs))]
          (when (and (> n 1000) (zero? (mod i 100))) (println " ", i))
          (doseq [j (range i)]
            (let [pair (pack size (nth nbrs i) (nth nbrs j))]
              (if-let [value (.get matches pair)]
                (.put matches pair (byte (inc value)))
                (.put matches pair (byte 0)))))))))
  matches)

;; #### Tying everything together.

(defn rev-compare
  "Reverse `compare` to sort in descending order."
  [a b]
  (* -1 (compare a b)))

(defn over
  "Generate a predicate used to select values over some limit."
  [lo]
  (fn [entry] (>= (.getValue entry) lo)))

(defn stop-after
  "Generate a predicate that assesses the pair-count information, stops
  the search when a pair with `hi` or more counts is found, and returns
  all pairs with `lo` or more counts."
  [size lo hi]
  (fn [matches]
    (if (.isEmpty matches)
      (do (println "no matches") nil)
      (let [max (first (sort rev-compare (.values matches)))]
        (println "matches at" max)
        (if (< max hi)
          (do (println "too few matches") nil)
          (letfn [(over [key]
                    (let [value (.get matches key)]
                      (>= value lo)))
                  (to-pair [key]
                    (let [value (.get matches key)]
                      [(unpack size key) value]))]
            (map to-pair (filter over (.keys matches)))))))))

(defn group-dump
  "The approach here uses a sample of 'pixel' values, with least significant
   bits discarded, as a locality sensitive hash.  By repeating the hashing
   multiple times, and counting how often the hash identifies particular
   pairs of images, we can find the closest pairs.

   Unfortunately the choice of mask, number of samples, and cut-off point
   are hard to automate completely - parameters need 'tweaking' by hand
   when used.

   For poor choices of parameters, memory use is prohibitive (even in normal
   use the implementation takes care to reduce memory and cpu use)."
  ([path n bits [lo hi] seed]
    (let [size (measure path n)
          stop (stop-after size lo hi)]
      (group-dump path n bits size stop (Random. seed) (TLongByteHashMap.))))
  ([path n bits size stop random matches]
    (if-let [results (stop matches)]
      results
      (recur path n bits size stop random
        (collect size matches
          (reduce-dump (make-hash n bits path random) [0 {}] path n))))))