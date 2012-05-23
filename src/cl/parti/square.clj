(ns cl.parti.square
  (:use (cl.parti random mosaic hsl))
  (:use clojure.math.numeric-tower))


; mosaic generation based on transforming a randomly selected square,
; located along a diagonal.  the first algorithm used here - gives good
; results, but difficult to quantify.

(def ^:private DELTA 2) ; range over which values shift in a single square
(def ^:private NORM 0.3) ; scale for converting from shift to colours


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
(defn- apply-2 [delta rows [x y]]
  (let [row (rows x)
        val (delta (row y))]
    (assoc rows x (assoc row y val))))

; apply a function to all tiles within the square
(defn- transform-square [[n diag rows] [delta [r1 r2]]]
  (let [[xlo xhi ylo yhi] (corners n diag r1 r2)
        xys (for [x (range xlo (inc xhi)) y (range ylo (inc yhi))] [x y])]
    [n diag (reduce #(apply-2 delta %1 %2) rows xys)]))

; this is the transform - we add/subtract a random amount from the value
(defn- make-delta [state]
  (let [[delta state] (range-closed DELTA state)]
    [#(+ delta %), state]))

; given a source of transforms, generate the parameters needed
; to call transform-square as a lazy stream
(defn- parameters [state]
  (lazy-seq
    (let [[r1 state] (uniform-open state)
          [r2 state] (uniform-open state)
          [delta state] (make-delta state)]
      (cons [delta [r1 r2]] (parameters state)))))

; apply the transform n times using random parameters
(defn- repeated-transform [n count state]
  (let [rows (vec (repeat n (vec (repeat n 0))))
        [diag state] (sign state)
        [n d r]
        (reduce transform-square [n diag rows]
          (take count
            (parameters state)))]
    r))

(defn square [options state]
  (let [n (:tile-number options)
        rows (repeated-transform n (expt n 2) state)
        norm (* NORM DELTA (expt n 0.8))]
    (normalize norm rows)))
