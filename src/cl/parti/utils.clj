(ns cl.parti.utils)


(defn error [& msg]
  (println (apply str msg))
  (System/exit 1))

(defn ?merge [map extra]
  (reduce (fn [map [k v]] (if (map k) map (assoc map k v)))
    map extra))

