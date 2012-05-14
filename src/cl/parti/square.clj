(ns cl.parti.square
  (:use (cl.parti random mosaic hsl))
  (:use clojure.math.numeric-tower))


; transform support -----------------------------------------------------------

; generate the corners of a square, given two random numbers
; and an orientation
(defn- square [n diag r1 r2]
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
  (let [[xlo xhi ylo yhi] (square n diag r1 r2)]
    xys (for [x (range xlo (inc xhi)) y (range ylo (inc yhi))] [x y])
    [n diag (reduce #(apply-2 transform %1 %2) rows xys)]))

; given a source of transforms, generate the parameters needed
; to call transform-square as a lazy stream
(defn- parameters [transform-factory state]
  (lazy-seq (let [[r1 state] (uniform-open state)
                  [r2 state] (uniform-open state)
                  [transform state] (transform-factory state)]
              (cons [transform [r1 r2]] (parameters transform-factory state)))))

; apply the transform n times using random parameters
(defn- repeated-transform [ndr n transform-factory state]
  (reduce transform-square ndr
    (take n (parameters transform-factory state))))

; this is the transform - we add/subtract a random amount from the value
(defn- make-delta [state]
  (let [[delta state] (range-closed -3 3 state)]
    [#(+ delta %), state]))


; render support --------------------------------------------------------------

(defn make-to-hsl [hue h-v-l]
  (fn [x]
    (let [x (/ x 2)] ; [-1 1] => [-0.5 0.5]
      [(fold (+ h x)) 1 (clip (+ 0.5 (* h-v-l x)))])))


; type ------------------------------------------------------------------------

(deftype Diagonal [options diag h-v-l hue rows]
  "
options  from cli
diag     -1 or 1 to select diagonal
h-v-l    -1 or 1 to select variation of lighntess with hue
hue      base hue [0-1)
rows     standard integer state representation
(rows of cols of int, starting at 0)
"
  Mosaic

  (transform [this state]
    (let [n (:tile-number options)
          k (:complexity options)
          rows (repeated-transform [n diag rows] k make-delta state)]
      (Diagonal options diag h-v-l hue rows)))

  (render [this]
    (defn to-hsl [x]
      (let [x (/ x 2)] ; [-1 1] => [-0.5 0.5]
        [(fold (+ h x)) 1 (clip (+ 0.5 (* h-v-l x)))]))
    (let [scale (* 0.5 (apply max (map abs (flatten rows))))
          rows-11 (map-rows (make-sigmoid scale) rows)])
    (map-rows to-hsl rows-11)))

(defn diagonal [options state]
  (let [[diag h-v-l state] (sign-2 state)
        [hue state] (uniform-open state)
        n (:tile-number options)
        rows (vec (repeat n (vec (repeat n 0))))]
    [(Diagonal options diag h-v-l hue rows) state]))
