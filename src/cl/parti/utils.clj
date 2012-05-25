(ns cl.parti.utils)


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
