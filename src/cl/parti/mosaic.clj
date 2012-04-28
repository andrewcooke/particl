(ns cl.parti.mosaic
  (:use (cl.parti random)))

; a mosaic is a [n a [row]] tuple, where row is [hsl] (one per column)
; and a is +/1 for the axis of symmetry

(defn mosaic [n a bg]
  [n a (vec (repeat n (vec (repeat n bg))))])

(defn square [n a r1 r2]
  (let [xlo (int (* n (min r1 r2)))
        xhi (int (* n (max r1 r2)))
        [ylo yhi] (if (> 0 a) [xlo xhi] [(- n xhi) (- n xlo)])]
    [xlo xhi ylo yhi]))

(defn transform-square [[n a rows] [t [r1 r2]]]
  (let [[xlo xhi ylo yhi] (square n a r1 r2)
        rows (reduce t rows (for [x (range xlo xhi)
                                  y (range ylo yhi)] [x y]))]
    [n a rows]))

(defn parameters [transform-factory state]
  (lazy-seq (let [[r1 state] (uniform-open state)
                  [r2 state] (uniform-open state)
                  [t state] (transform-factory state)]
              (cons [t [r1 r2]] (parameters transform-factory state)))))

(defn repeated-transform [mosaic n transform-factory state]
  (reduce transform-square mosaic
    (take n (parameters transform-factory state))))

(defn bracket-interpose [sep col]
  (concat [sep] (interpose sep col) [sep]))

(defn no-interpose [sep col]
  col)

(defn print-mosaic [print-colours convert [n a rows] scale [colour width]]
  (let [size (+ (* n scale) (* (inc n) width))
        horizontal (repeat width (repeat size (convert colour)))
        vertical (repeat width colour)
        assemble (if (> width 0) bracket-interpose no-interpose)]
    (print-colours size
      (apply concat
        (assemble horizontal
          (for [row rows]
            (repeat scale
              (apply concat
                (assemble vertical
                  (for [col row]
                    (repeat scale (convert col))))))))))))

(defn print-identity [size colours]
  colours)
