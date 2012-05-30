(ns ^{:doc "

Utilities used by the render and display functions.

"
      :author "andrew@acooke.org"}
  cl.parti.mosaic
  (:use (cl.parti random hsl))
  (:import java.lang.Math)
  (:use clojure.math.numeric-tower))

;; ## Mosaic expansion

(defn bracket-interpose
  "Extend the standard `interpose` function, adding an additional copy
  of the padding at either end of the sequence."
  [sep col]
  (concat [sep] (interpose sep col) [sep]))

(defn no-interpose
  "A replacement for `interpose` that discards the additional padding.  Used
  when no background is required."
  [sep col]
  col)

(defn expand-mosaic
  "The mosaic is generated using a single value / pixel per tile.  This is
  all that is necessary during generation, since each tile is uniform.
  However, during rendering, each tile must be expanded, and the background
  grid (or 'grout') introduced.

  This is achieved here by `repeat`ing tile pixels and then `interpose`ing
  the background."
  [n scale colour width rows]
  (let [size (+ (* n scale) (* (inc n) width))
        horizontal (repeat width (repeat size colour))
        vertical (repeat width colour)
        assemble (if (> width 0) bracket-interpose no-interpose)]
    (apply concat
      (assemble horizontal
        (for [row rows]
          (repeat scale
            (apply concat
              (assemble vertical
                (for [col row]
                  (repeat scale col))))))))))

;; ## General utilities

(defn map-rows
  "The 2d mosaic is generally modelled as sequences of sequences.  This
  defines a shape-preserving map over the values."
  [f rows]
  (for [row rows]
    (for [col row] (f col))))

(defn print-rows
  "This defines a simple API used to print the mosaic, implemented by the
  `cl.parti.png` module.  Two functions are used - one converts each
  pixel; the other is passed the mosaic dimension and the converted values."
  [[printer convert] rows]
  (printer (count rows) (map-rows convert rows)))

;; ## Range reduction

(defn make-sigmoid
  "Given a scale factor, generate a function that maps values to (-1 1)."
  [k]
  (fn [x]
    (let [x (/ x k)]
      (- (/ 2 (+ 1 (Math/exp (- x)))) 1))))

(defn- de-mean
  "Subtract the mean value from a mosaic (2D nested sequence) of floats."
  [rows]
  (let [flat (flatten rows)
        mean (/ (apply + flat) (count flat))]
    (map-rows #(- % mean) rows)))

(defn normalize
  "Normalize a mosaic (2D nested sequence) of floats by subtracting the mean
  and then using a sigmoid to map into (-1 1)."
  [norm rows]
  (let [rows (de-mean rows)]
    (map-rows (make-sigmoid norm) rows)))

(defn cumulate
  ([values] (cumulate (distinct (sort values)) (frequencies values) 0 {}))
  ([ordered freq sum histogram]
    (let [value (first ordered)
          ordered (rest ordered)
          n (freq value)
          histogram (assoc histogram value sum)]
      (if (seq ordered)
        (recur ordered freq (+ n sum) histogram)
        (apply merge {} (map (fn [[k v]] [k (/ v sum)]) histogram))))))

(defn equalize
  [rows]
  (let [hist (cumulate (flatten rows))]
    (map-rows (fn [x] (- (* 2 (hist x)) 1)) rows)))
