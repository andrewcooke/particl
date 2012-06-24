(ns ^{:doc "

Functions used in the output sections of the pipeline.

"
      :author "andrew@acooke.org"}
  cl.parti.output
  (:use clojure.java.io)
  (:use (cl.parti png utils hsl random)))


;; ## Pre-editor functions

(defn noise
  [n]
  (let [m (bit-shift-right n 2)]
    (fn [[norm rows state]]
      (let [[rows state]
            (map-state-2
              (fn [state value]
                (let [[delta state] (rand-bits-symmetric m state)]
                  [(+ delta value) state]))
              state rows)]
        [norm rows state]))))

(defn no-pre-editor
  [n]
  (fn [[norm rows state]]
    [norm rows state]))

;; ## Normalize functions
;;
;; Reduce the range of values in the internal representation.

;; #### Soft normalisation

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
    (map-2 #(- % mean) rows)))

(defn normalize-sigmoid
  "Subtract the mean and then normalize with a sigmoid, giving a 'soft'
  appearance and reduced dynamic range."
  [[norm rows state]]
  (let [rows (de-mean rows)]
    [(map-2 (make-sigmoid norm) rows) state]))

;; #### Hard normalisation

(defn cumulate
  "Calculate the CDF of distribution (recurrence) and then apply that to
  the image, mapping values to be equi-distributed."
  ([values] (cumulate (distinct (sort values)) (frequencies values) 0 {}))
  ([ordered freq sum histogram]
    (let [value (first ordered)
          ordered (rest ordered)
          n (freq value)
          histogram (assoc histogram value sum)]
      (if (seq ordered)
        (recur ordered freq (+ n sum) histogram)
        (if (zero? sum)
          {value 0.5} ; special case where single value in all cells
          (apply merge {} (map (fn [[k v]] [k (/ v sum)]) histogram)))))))

(defn normalize-histogram
  "Normalize with histogram-equalisation, giving a 'hard' appearance and
  maximum dynamic range."
  [[norm rows state]]
  (let [hist (cumulate (flatten rows))]
    [(map-2 (fn [x] (- (* 2 (hist x)) 1)) rows) state]))

;; ## Render functions
;;
;; Convert the internal representation (2D sequence of floats) to HSL pixels.

(def
  ^{:doc ""}
  LIGHTNESS 0.8)

(def
  ^{:doc ""}
  DAZZLE 0.4)

(defn- floats-to-hsl
  "Convert a mosaic (2D nested sequences) of normalized (-1 1) floats to
 HSL triplets.

 The mosaic is based on a (randomly selected) fully saturated hue.  The
 float values then describe an offset, in both hue and lightness, relative
 to that.  This approach correlates colour and lightness shifts, making the
 structure in the mosaic more likely to visible to those with restricted
 colour vision."
  [mono lightness dazzle h-v-l hue rows-11]
  (defn to-hsl [x]
    (let [x (/ x 2)] ; [-1 1] => [-0.5 0.5]
      [(if mono 0 (fold (+ hue (* dazzle x))))
       (if mono 0 1)
       (clip (+ 0.5 (* lightness h-v-l x)))]))
  (map-2 to-hsl rows-11))

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

(defn render-floats
  "Convert and expand the internal float-based representation, generating a
full mosaic of HSL values.

This increases the variation among images, consuming 10 bits of state."
  [n scale colour width mono raw]
  (fn [[rows state]]
    (let [[rotate state] (if raw [1 state] (rand-sign state))
          [h-v-l state] (if raw [1 state] (rand-sign state))
          [hue state] (if raw [(/ raw 255.0) state] (rand-real 1 state))
          mosaic
          (expand-mosaic n scale colour width
            (floats-to-hsl mono LIGHTNESS DAZZLE h-v-l hue
              (if (= 1 rotate) rows (rotate-2 n rows))))]
      (println hue h-v-l)
      [colour mosaic state])))


;; ## Post-editor functions

(defn- to-vec-2d
  "Convert the nested 2D sequences to nested 2D vectors to support
efficient random-access updates."
  [rows]
  (vec (for [row rows] (vec row))))

(defn- set-pixel
  "Update the given image, setting a pixel to the background colour."
  [bg]
  (fn [mosaic [x y]]
    (assoc mosaic x (assoc (mosaic x) y bg))))

(defn- darken-pixel
  "Update the given image, darkening a given pixel.

This was an alternative to `set-pixel` that didn't look so good."
  [bg]
  (fn [mosaic [x y]]
    (let [p ((mosaic x) y)]
      (assoc mosaic x (assoc (mosaic x) y (lighten 0.5 p))))))

(defn- corner-pixels
  "Generate a function which, given a tile location and corner index,
returns a list of pixel indices that describe a triangular 'corner'."
  [n scale width]
  (let [size (int (/ scale 3))
        delta (dec scale)]
    (fn [[x y] corner]
      (let [[x y] (map #(+ (* % scale) (* (inc %) width)) [x y])]
        (for [i (range size) j (range (- size i))]
          [(+ x (if (zero? (bit-and corner 1)) i (- delta i)))
           (+ y (if (zero? (bit-and corner 2)) j (- delta j)))])))))

;; #### The corners editor function.

(defn- rand-corner
  "Generate a function which, given an existing list of pixels and a tile
location, appends pixels for a random corner on that tile."
  [n scale width]
  (let [cp (corner-pixels n scale width)]
    (fn [[pixels state] [x y]]
      (let [[corner state] (rand-bits 4 state)]
        [(cons (cp [x y] corner) pixels) state]))))

(defn corners
  "Mark a random corner of each tile."
  [n scale width]
  (fn [[bg mosaic state]]
    (let [mosaic (to-vec-2d mosaic)
          tiles (for [x (range n) y (range n)] [x y])
          [pixels state] (reduce (rand-corner n scale width) [nil state] tiles)]
      [(reduce (set-pixel bg) mosaic (flatten-1 pixels)) state])))

;; #### The hole editor function.

(defn- rand-hole
  "Choose random 'holes' on the corners of the mosaic.

More exactly, at generate a function which, given a point, randomly
decides whether or not it should contain a hole.  If so, list the
pixels that need to be set (affecting four neighbouring tiles)."
  [n scale width rate]
  (let [cp (corner-pixels n scale width)]
    (fn [[pixels state] [x y]]
      (let [[hole state] (rand-bits (Math/abs rate) state)]
        [(cons
           (if (= (zero? hole) (> rate 0))
             (concat
               (cp [x y] 3)
               (cp [(inc x) y] 2)
               (cp [x (inc y)] 1)
               (cp [(inc x) (inc y)] 0))
             nil)
           pixels) state]))))

(defn holes
  "Add random 'punch holes' to the image.

The `rate` parameter means the following:

* 0: disabled
* n: a random 'hole' once every n interstices, on average
* -ve: random holes everywhere, except random omissions every abs(n)."
  [n scale width rate]
  (fn [[bg mosaic state]]
    (if (zero? rate)
      [bg mosaic state]
      (let [mosaic (to-vec-2d mosaic)
            centres (for [x (range (dec n)) y (range (dec n))] [x y])
            [pixels state]
            (reduce (rand-hole n scale width rate) [nil state] centres)]
        [bg (reduce (set-pixel bg) mosaic (flatten-1 pixels)) state]))))

(defn no-post-editor
  "A dummy editor function for when nothing is required."
  [[bg mosaic state]]
  [mosaic state])


;; ## Display functions
;;
;; These take the mosaic (a 2D sequence of HSL values) and save or display
;; it somewhere.

(defn print-rows
  "This defines a simple API used to print the mosaic, implemented by the
`cl.parti.png` module.  Two functions are used - one converts each
pixel; the other is passed the mosaic dimension and the converted values."
  [[printer convert] rows]
  (printer (count rows) (map-2 convert rows)))

(defn file-display
  "Save the mosaic (2D nested sequences of HSL values) as a PNG format
image, using the functions defined in `cl.parti.png`.

Small mosaics (side of 16 or less) are indexed to reduce file size.
An arbitrary mosaic of size 16 can contain 257 different colours (one
for each tile, plus backgroud).  However, mosaics are symmetrical, so
the maximum number of colours is significantly less.

It might pay to count the number of distinct colours if we can extend to
20x20 mosaics (corresponding to the 'hash' style)."
  [path]
  (fn [[bg rows state] n arg]
    (let [path
          (cond
            (< -1 (.indexOf path "%d")) (format path n)
            (< -1 (.indexOf path "%s")) (format path arg)
            :else (do (when (= 1 n)
                        (printf "WARNING: No '%%d' or '%%s' in --output\n"))
                    path))]
      (with-open [os (output-stream path)]
        (let [n (count rows)
              print (if (< n 17) (print-png-indexed os) (print-png-8-bit os))]
          (print-rows print rows))))))

(defn gui-display
  "Display the image directly to the user (unimplemented)."
  [& args]
  (error "gui display"))
