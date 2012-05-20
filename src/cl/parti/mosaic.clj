(ns cl.parti.mosaic
  (:use (cl.parti random hsl))
  (:import java.lang.Math)
  (:use clojure.math.numeric-tower))


(def OVERSHOOT 1.5)


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

; normalize rows of floats so that they are [-1 1]

(defn- clip-11 [x]
  (cond
    (< x -1) -1
    (> x 1) 1
    :else x))

(defn make-sigmoid [k]
  (fn [x]
    (let [x (/ x k)]
      (clip-11 (* OVERSHOOT (- (/ 2 (+ 1 (Math/exp (- x)))) 1))))))

(defn- de-mean [rows]
  (let [flat (flatten rows)
        mean (/ (apply + flat) (count flat))]
    (map-rows #(- % mean) rows)))

(defn normalize [norm rows]
  (let [rows (de-mean rows)]
    (map-rows (make-sigmoid norm) rows)))

; convert rows of floats to rows of hsl values.

(defn floats-to-hsl [options lightness h-v-l hue rows-11]
  (defn to-hsl [x]
    (let [x (/ x 2)] ; [-1 1] => [-0.5 0.5]
      [(fold (+ hue x)) 1 (clip (+ 0.5 (* lightness h-v-l x)))]))
  (let [n (:tile-number options)
        scale (:tile-size options)
        colour (:border-colour options)
        width (:border-width options)
        hsl (map-rows to-hsl rows-11)]
    (expand-mosaic n scale colour width hsl)))
