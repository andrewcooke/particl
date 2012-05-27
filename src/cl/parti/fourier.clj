(ns ^{:doc "

An alternative renderer to `square`.  Currently broken.

"
      :author "andrew@acooke.org"}
  cl.parti.fourier
  (:use (cl.parti random mosaic hsl))
  (:use clojure.math.numeric-tower))


(def ^:private NORM 2) ; scale for converting from shift to colours


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
    (let [[phase state] (rand-real (* 2 Math/PI) state)
          [amplitude state] (rand-real 1)]
      (cons [phase amplitude] (coeff state)))))

; TODO - ignores diag!

(defn fourier [n]
  (fn [state]
    (let [[diag state] (rand-sign state)
          coeffs (vec (take (dec n) (coeff state)))
          rows (coeffs-to-rows n coeffs)]
      (normalize NORM rows))))
