(ns ^{:doc "

Functions used in the output sections of the pipeline.

"
      :author "andrew@acooke.org"}
  cl.parti.output
  (:use clojure.java.io)
  (:use (cl.parti png mosaic utils hsl random)))


;; ## Display functions

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
  (let [index (atom 0)]
    (fn [rows]
      (when (and (= @index 1) (= -1 (.indexOf path "%d")))
        (printf "WARNING: Multiple output images but no '%%d' in --output\n"))
      (with-open [os (output-stream (format path @index))]
        (let [n (count rows)
              print (if (< n 17) (print-png-indexed os) (print-png-8-bit os))]
          (print-rows print rows)))
      (swap! index inc))))

(defn gui-display
  "Display the image directly to the user (unimplemented)."
  [& args]
  (error "gui display"))

;; ## Render functions

(def
  ^{:doc "The strength of changes in luminosity, relative to changes in hue."}
  LIGHTNESS 0.5)

(defn- floats-to-hsl
  "Convert a mosaic (2D nested sequences) of normalized (-1 1) floats to
  HSL triplets.

  The mosaic is based on a (randomly selected) fully saturated hue.  The
  float values then describe an offset, in both hue and lightness, relative
  to that.  This approach correlates colour and lightness shifts, making the
  structure in the mosaic more likely to visible to those with restricted
  colour vision."
  [mono lightness h-v-l hue rows-11]
  (defn to-hsl [x]
    (let [x (/ x 2)] ; [-1 1] => [-0.5 0.5]
      [(if mono 0 (fold (+ hue x)))
       (if mono 0 1)
       (clip (+ 0.5 (* lightness h-v-l x)))]))
  (map-rows to-hsl rows-11))

(defn- rotate-rows
  "Rotate the rows.

  Rendered images are diagonally symmetric so have two distinct orientations."
  [n rows]
  (let [m (dec n)]
    (for [j (range n)]
      (for [i (range n)]
        (nth (nth rows (- m i)) j)))))

(defn render-floats
  "Convert and expand the internal float-based representation, generating a
  full mosaic of HSL values.

  This increases the variation among images, consuming 10 bits of state."
  [n scale colour width mono raw]
  (fn [[rows state]]
    (let [[rotate state] (if raw [1 state] (rand-sign state))
          [h-v-l state] (if raw [1 state] (rand-sign state))
          [hue state] (if raw [(/ raw 255.0) state] (rand-real 1 state))]
      (expand-mosaic n scale colour width
        (floats-to-hsl mono LIGHTNESS h-v-l hue
          (if (= 1 rotate) rows (rotate-rows n rows)))))))

(defn normalize-sigmoid
  [[norm rows state]]
  [(normalize norm rows) state])

(defn normalize-histogram
  [[norm rows state]]
  [(equalize rows) state])
