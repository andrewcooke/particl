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

(defn map-rows
  "The 2D mosaic is generally modelled as sequences of sequences.  This
  defines a shape-preserving map over the values."
  [f rows]
  (for [row rows]
    (for [col row] (f col))))

