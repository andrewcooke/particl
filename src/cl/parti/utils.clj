(ns ^{:doc "

Various utilitiy functions.

"
      :author "andrew@acooke.org"}
  cl.parti.utils
  (:import org.apache.commons.codec.binary.Hex))


(defn error
  "Display an error message and exit."
  [& msg]
  (println (apply str msg))
  (System/exit 1))


(defn ?merge
  "Merge the contents of `extra` with `map`.  The result contains
  all the key-value pairs from `map`, plus those for additional keys
  from `extra`."
  [map extra]
  (reduce (fn [map [k v]] (if (map k) map (assoc map k v)))
    map extra))


(defn unsign-byte
  "Convert an signed byte (the Java native type) to an integer value
  whose bits in the least significant byte are identical to the bits
  in the original byte.

  This is *not* eqiuvalent to taking the absolute value.  For example,
  `-1` becomes `255`."
  [b]
  (if (< b 0)
    (+ 256 b)
    b))

(defn sign-byte
  "Convert an integer value, assumed to be in the range [0 256), to
  a signed byte (the Java native type) whose bits are identical to the bits
  in the least significant byte of the original value.

  For example, 255 becomes -1."
  [b]
  (byte (if (> b 127)
          (- b 256)
          b)))


(defn sgn
  "Return the sign of `x` (a value in [-1 1])."
  [x]
  (cond (< x 0) -1 (> x 0) 1 :else 0))


(def ^:private ^{:doc "A utility instance for managing hexadecimal values."}
  HEX (Hex.))

(defn parse-hex
  "Convert a string of hex digits to an array of bytes."
  [hex]
  (.decode HEX hex))

(defn format-hex
  "Convert an array of bytes to a string of hex digits."
  [hex]
  (Hex/encodeHexString hex))


(defn flatten-1
  "Reduce nested lists once level."
  [list]
  (apply concat list))

;; ## Data Structures
;;
;; Much of the code here deals with a 'mosaic' in various forms.  This is
;; usually represented as a sequence of sequences.  The *outer* sequence
;; (the mosaic) collects the rows; an *inner* sequences (a row) contains the
;; pixel values.  So if the mosaic is represented as a float (called the
;; 'internal representation') then a row is a sequence of floats.  If the
;; mosaic is represented as HSL triplets then a row is a sequence of triplets.
;;
;; The structure maps directly to the PNG format, so the first row in the
;; mosaic is the top row of the image.  We can use this to define coordinates:
;; `y` is the row number (from the top) and `x` is the pixel number (from
;; the right).
;;
;; An exception to the above is in the `analysis` module, where triangular
;; samples are extracted as sequences of bytes.

(defn map-rows
  "The 2D mosaic is generally modelled as sequences of sequences.  This
  defines a shape-preserving map over the values."
  [f rows]
  (for [row rows]
    (for [col row] (f col))))

(defn nth-2
  [colln [x y]]
  (nth (nth colln y) x))

(defn vapply-2
  "Apply function `f` to the value in the 2D nested sequences `rows` at
  `[x,y]` (this assumes nested vectors)."
  [f rows [x y]]
  (let [row (rows y)
        val (f (row x))]
    (assoc rows y (assoc row x val))))

(defmacro dopar [seq-expr & body]
  (assert (= 2 (count seq-expr)) "single pair of forms in sequence expression")
  (let [[k v] seq-expr]
    `(apply await
       (for [k# ~v]
         (let [a# (agent k#)]
           (send a# (fn [~k] ~@body))
           a#)))))
