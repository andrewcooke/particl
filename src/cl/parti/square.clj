(ns cl.parti.square
  (:use (cl.parti random mosaic hsl))
  (:use clojure.math.numeric-tower))


; mosaic generation based on transforming a randomly selected square,
; located along a diagonal.  the first algorithm used here - gives good
; results, but difficult to quantify.

(def DELTA 2) ; range over which values shift in a single square
(def LIGHTNESS 0.75) ; relative strength of l changes, relative to h
(def NORM 0.5) ; scale for converting from shift to colours


; transform support -----------------------------------------------------------

; generate the corners of a square, given two random numbers
; and an orientation
(defn- corners [n diag r1 r2]
  (let [m (dec n)
        side (int (* n (expt r1 1.7)))
        xlo (int (* (- n side) r2))
        xhi (+ xlo side)
        [ylo yhi] (if (> 0 diag) [xlo xhi] [(- m xhi) (- m xlo)])]
    [xlo xhi ylo yhi]))

; apply a function to a value in the nested vectors
(defn- apply-2 [transform rows [x y]]
  (let [row (rows x)
        val (transform (row y))]
    (assoc rows x (assoc row y val))))

; apply a function to all tiles within the square
(defn- transform-square [[n diag rows] [transform [r1 r2]]]
  (let [[xlo xhi ylo yhi] (corners n diag r1 r2)
        xys (for [x (range xlo (inc xhi)) y (range ylo (inc yhi))] [x y])]
    [n diag (reduce #(apply-2 transform %1 %2) rows xys)]))

; given a source of transforms, generate the parameters needed
; to call transform-square as a lazy stream
(defn- parameters [transform-factory state]
  (lazy-seq
    (let [[r1 state] (uniform-open state)
          [r2 state] (uniform-open state)
          [transform state] (transform-factory state)]
      (cons [transform [r1 r2]] (parameters transform-factory state)))))

; apply the transform n times using random parameters
(defn- repeated-transform [ndr n transform-factory state]
  (let [[n d r]
        (reduce transform-square ndr
          (take n
            (parameters transform-factory state)))]
    r))

; this is the transform - we add/subtract a random amount from the value
(defn- make-delta [state]
  (let [[delta state] (range-closed (- DELTA) DELTA state)]
    [#(+ delta %), state]))

(defn- de-mean [rows]
  (let [flat (flatten rows)
        mean (/ (apply + flat) (count flat))]
    (map-rows #(- % mean) rows)))


; type ------------------------------------------------------------------------

;options  from cli
;diag     -1 or 1 to select diagonal
;h-v-l    -1 or 1 to select variation of lighntess with hue
;hue      base hue [0-1)
;rows     standard integer state representation
;         (rows of cols of int, starting at 0)
(defrecord Square [options diag h-v-l hue rows]

  Mosaic

  (transform [this state]
    (let [n (:tile-number options)
          k (:complexity options)
          rows (repeated-transform [n diag rows] (* k (expt n 2)) make-delta state)]
      (Square. options diag h-v-l hue rows)))

  (render [this]
    (let [n (:tile-number options)
          k (:complexity options)]
      (defn to-hsl [x]
        (let [x (/ x 2)] ; [-1 1] => [-0.5 0.5]
          [(fold (+ hue x)) 1 (clip (+ 0.5 (* LIGHTNESS h-v-l x)))]))
      (let [scale (* NORM DELTA (* k (expt n 0.8)))
            rows-11 (map-rows (make-sigmoid scale) (de-mean rows))
            scale (:tile-size options)
            colour (:border-colour options)
            width (:border-width options)]
        (expand-mosaic n scale colour width (map-rows to-hsl rows-11))))))

(defn square [options state]
  (let [[diag h-v-l state] (sign-2 state)
        [hue state] (uniform-open state)
        n (:tile-number options)
        rows (vec (repeat n (vec (repeat n 0))))]
    [(Square. options diag h-v-l hue rows) state]))
