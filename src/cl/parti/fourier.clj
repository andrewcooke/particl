(ns cl.parti.fourier
  (:use (cl.parti random mosaic hsl))
  (:use clojure.math.numeric-tower))


(def ^:private NORM 3) ; scale for converting from shift to colours


(defn- expand-term [x i [phase amplitude]]
  (* amplitude (Math/cos (+ phase (* 2 Math/PI (/ x (inc i)))))))

(defn- expand-pixel [x n coeffs]
  (apply + (for [i (range (dec n))] (expand-term x i (coeffs i)))))

(defn- expand-row [n coeffs]
  (for [x (range n)] (expand-pixel x n coeffs)))

(defn- coeffs-to-rows [n coeffs]
  (let [row (expand-row n coeffs)]
    (for [a row] (for [b row] (+ a b)))))

(defn- coeff [state]
  (lazy-seq
    (let [[phase state] (range-closed Math/PI state)
          [amplitude state] (uniform-open state)]
      (cons [phase amplitude] (coeff state)))))

(defn fourier [options state]
  (let [n (:tile-number options)
        [diag state] (sign state)
        coeffs (vec (take (dec n) (coeff state)))
        rows (coeffs-to-rows n coeffs)]
    (normalize NORM rows)))
