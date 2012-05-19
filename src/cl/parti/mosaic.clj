(ns cl.parti.mosaic
  (:use (cl.parti random hsl))
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
  (expand [mosaic]))


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

(defn- de-mean [rows]
  (let [flat (flatten rows)
        mean (/ (apply + flat) (count flat))]
    (map-rows #(- % mean) rows)))

(defn floats-to-hsl [options norm lightness h-v-l hue rows]
  (defn to-hsl [x]
    (let [x (/ x 2)] ; [-1 1] => [-0.5 0.5]
      [(fold (+ hue x)) 1 (clip (+ 0.5 (* lightness h-v-l x)))]))
  (let [rows-11 (map-rows (make-sigmoid norm) (de-mean rows))
        n (:tile-number options)
        scale (:tile-size options)
        colour (:border-colour options)
        width (:border-width options)]
    (expand-mosaic n scale colour width (map-rows to-hsl rows-11))))
