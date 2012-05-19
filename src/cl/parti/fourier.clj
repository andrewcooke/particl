(ns cl.parti.fourier
  (:use (cl.parti random mosaic hsl))
  (:use clojure.math.numeric-tower))


(def DELTAX 2) ; range over which values shift in a single square
(def LIGHTNESSX 0.75) ; relative strength of l changes, relative to h
(def NORMX 0.5) ; scale for converting from shift to colours


; render support --------------------------------------------------------------

(defn perm [coeffs]
  (if-let [coeffs (seq coeffs)]
    (let [a (first coeffs)]
      (concat
        (for [b (perm (rest coeffs))] (cons a b))
        (for [b (perm (rest coeffs))] (cons (- a) b))))
    [[]]))

(defn- expand-row [coeffs]
  (map (partial apply + ) (perm coeffs)))

(defn- coeffs-to-rows [coeffs]
  (let [row (expand-row coeffs)]
    (for [a row] (for [b row] (+ a b)))))


; transform support -----------------------------------------------------------

(defn- ranges [state]
  (lazy-seq
    (let [[range state] (range-closed DELTAX state)]
      (cons range (ranges state)))))


; type ------------------------------------------------------------------------

; we actually use square waves rather than sines, but the idea is the same -
; we skip zeroth order and have single wavelength to n/2 wavelengths, each of
; amplitude +/- DELTA
(defrecord Fourier [options diag h-v-l hue coeffs]

  Mosaic

  (transform [this state]
    (let [n (:tile-number options)]
      (Fourier. options diag h-v-l hue
        (take (dec n) (ranges state)))))

  (expand [this]
    (println coeffs)
    (let [n (:tile-number options)
          norm (* NORMX DELTAX (expt (dec n) 0.5))
          rows (coeffs-to-rows coeffs)]
      (println rows)
      (floats-to-hsl options norm LIGHTNESSX h-v-l hue rows))))

(defn fourier [options state]
  (let [[diag h-v-l state] (sign-2 state)
        [hue state] (uniform-open state)
        n (:tile-number options)
        coeffs (vec (repeat n 0))]
    [(Fourier. options diag h-v-l hue []) state]))
