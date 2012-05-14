(ns cl.parti.mosaic
  (:use (cl.parti random))
  (:import java.lang.Math)
  (:use clojure.math.numeric-tower))


; we define an interface that can be implemented in various ways.  in all
; cases the mosaic is created from a set of options and random state,
; transformed using random state, and then rendered as a 2d-array of hsl
; values.

; constructors are not part of the protocol but have the form
; (defn constructor [options state] ...) and return [instance state]

(defprotocol Mosaic
  (transform [mosaic state])
  (render [mosaic]))

; general utilities -----------------------------------------------------------

(defn bracket-interpose [sep col]
  (concat [sep] (interpose sep col) [sep]))

(defn no-interpose [sep col]
  col)

; expand a set of rows of cols of hsls into a mosaic, repeating pixels
; and adding borders
(defn expand-mosaic [n scale colour width rows]
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

(defn map-rows [f rows]
  (for [row rows]
    (for [col row] (f col))))

(defn print-rows [[printer convert] rows]
  (printer (count rows) (map-rows convert rows)))


; float rows ------------------------------------------------------------------

; often it's useful to represent a mosaic as a set of rows of float values.
; these utilities help with conversion to hsl.

(defn make-sigmoid [k]
  (fn [x]
    (let [x (/ x k)]
      (- (/ 2 (+ 1 (Math/exp (- x)))) 0.5))))







;; a mosaic is a [n a b [row]] tuple, where:
;; - n is the number of pixels on a side,
;; - a is +/-1 for the axis of symmetry,
;; - b is +/-1 for colour/lightness correlation (for colourblind),
;; - row is [hsl] (one per column).
;
;(defn mosaic [n a b bg]
;  [n a b (vec (repeat n (vec (repeat n bg))))])
;
;; random orientation, colour correlation, and saturated colour
;(defn random-mosaic [n state]
;  (let [[a b state] (sign-2 state)
;        [h state] (uniform-open state)]
;    [(mosaic n a b [h 1 0.5]) state]))
;
;; generate the corners of a square, given two random numbers
;; and an orientation
;;(defn square [n a r1 r2]
;;  (let [m (dec n)
;;        xlo (int (* n (min r1 r2)))
;;        xhi (int (* n (max r1 r2)))
;;        [ylo yhi] (if (> 0 a) [xlo xhi] [(- m xhi) (- m xlo)])]
;;    [xlo xhi ylo yhi]))
;(defn square [n a r1 r2]
;  (let [m (dec n)
;        side (int (* n (expt r1 1.7)))
;        xlo (int (* (- n side) r2))
;        xhi (+ xlo side)
;        [ylo yhi] (if (> 0 a) [xlo xhi] [(- m xhi) (- m xlo)])]
;    [xlo xhi ylo yhi]))
;
;; apply a function to a value in the nested vectors
;(defn apply-2 [rows [x y] f]
;  (let [row (rows x)
;        val (f (row y))]
;    (assoc rows x (assoc row y val))))
;
;; apply a function to all tiles within the square
;(defn transform-square [[n a b rows] [t [r1 r2]]]
;  (let [[xlo xhi ylo yhi] (square n a r1 r2)]
;    [n a b (reduce
;      (fn [r xy] (apply-2 r xy t)) rows
;      (for [x (range xlo (inc xhi))
;            y (range ylo (inc yhi))] [x y]))]))
;
;; given a source of transforms, generate the parameters needed
;; to call transform-square as a lazy stream
;(defn parameters [transform-factory state]
;  (lazy-seq (let [[r1 state] (uniform-open state)
;                  [r2 state] (uniform-open state)
;                  [t state] (transform-factory state)]
;              (cons [t [r1 r2]] (parameters transform-factory state)))))
;
;; apply the transform n times using random parameters
;(defn repeated-transform [mosaic n transform-factory state]
;  (reduce transform-square mosaic
;    (take n (parameters transform-factory state))))
;
;(defn bracket-interpose [sep col]
;  (concat [sep] (interpose sep col) [sep]))
;
;(defn no-interpose [sep col]
;  col)
;
;; format the mosaic, sending the results to a given function:
;; - print receives the size (total pixels per side) and transformed rows
;; - convert transforms each row element before calling print
;; - [n a b rows] is the mosaic
;; - scale is the number of pixels on the side of a single tile
;; - [colour width] is the grout (inter-tile)/border
;(defn print-mosaic [[print convert] [n a b rows] scale [colour width]]
;  (let [size (+ (* n scale) (* (inc n) width))
;        horizontal (repeat width (repeat size (convert colour)))
;        vertical (repeat width (convert colour))
;        assemble (if (> width 0) bracket-interpose no-interpose)]
;    (print size
;      (apply concat
;        (assemble horizontal
;          (for [row rows]
;            (repeat scale
;              (apply concat
;                (assemble vertical
;                  (for [col row]
;                    (repeat scale (convert col))))))))))))
;
;(defn print-identity [size colours]
;  colours)
