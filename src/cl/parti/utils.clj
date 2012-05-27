(ns ^{:doc "


"
      :author "andrew@acooke.org"}
  cl.parti.utils
  (:import org.apache.commons.codec.binary.Hex))


(defn error [& msg]
  (println (apply str msg))
  (System/exit 1))

(defn ?merge [map extra]
  (reduce (fn [map [k v]] (if (map k) map (assoc map k v)))
    map extra))

(defn unsign-byte [b]
  (if (< b 0)
    (+ 256 b)
    b))

(defn sign-byte [b]
  (byte (if (> b 127)
          (- b 256)
          b)))

(defn sgn [x]
  (cond (< x 0) -1 (> x 0) 1 :else 0))


(def ^{:doc "A utility for parsing hexadecimal values."} HEX (Hex.))

(defn parse-hex [hex]
  (.decode HEX hex))

(defn format-hex [hex]
  (Hex/encodeHexString hex))
