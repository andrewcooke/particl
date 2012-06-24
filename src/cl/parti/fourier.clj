(ns ^{:doc "

An alternative builder to `square` and `rectangle`.

"
      :author "andrew@acooke.org"}
  cl.parti.fourier
  (:use (cl.parti random output utils))
  (:use clojure.math.numeric-tower))


(def ^:private NORM 0.75) ; scale for converting from shift to colours


(defn- expand-term [x i [phase amplitude]]
  (* amplitude (Math/sin (+ phase (* 2 Math/PI (/ x (inc i)))))))

(defn- expand-pixel [x n coeffs]
  (apply + (for [i (range (dec n))] (expand-term x i (coeffs i)))))

(defn- expand-row [n coeffs]
  (for [x (range n)] (expand-pixel x n coeffs)))

(defn- coeffs-to-rows [n coeffs]
  (let [row (expand-row n coeffs)]
    (for [a row] (for [b row] (+ a b)))))

;(defn- coeff [state]
;  (lazy-seq
;    (let [[phase state] (rand-real (* 2 Math/PI) state)
;          [amplitude state] (rand-real 1)]
;      (cons [phase amplitude] (coeff state)))))

(defn- pink-coeffs
  ([n state] (pink-coeffs [] n n state))
  ([acc i n state]
    (if (zero? i)
      acc
      (let [j (inc (- n i))
            [phase state] (rand-real (* 2 Math/PI) state)
            [amplitude state] (rand-real (Math/sqrt j) state)]
        (recur (conj acc [phase amplitude]) (dec i) n state)))))

(defn fourier [n]
  (fn [state]
    (let [[diag state] (rand-sign state)
          coeffs (pink-coeffs n state)
          rows (coeffs-to-rows n coeffs)
          norm (* NORM (apply max (flatten (map-2 max rows))))]
      [norm rows state])))
