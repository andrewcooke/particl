(ns cl.parti.utils)


(defn error [& msg]
  (println (apply str msg))
  (System/exit 1))
