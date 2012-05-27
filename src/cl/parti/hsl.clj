(ns ^{:doc "

A simple library for manipulating HSL values.  HSL is a 'colour space'
similar to RGB or HSV.  It contains three values (hue, saturation and
lightness) which are represented here as a triplet of floats in the range
[0-1].

HSL is used because the natural (single components) transformations give
better support for colourblind users.  At maximum saturation, varying
luminosity goes from black, through colour, to white.  In contrast, HSV
goes only from black to fully saturated colour.

"
      :author "andrew@acooke.org"}
  cl.parti.hsl
  (:use (cl.parti random utils))
  (:use clojure.math.numeric-tower))


(defn clip
  "Clip a value to lie within the range [0-1]."
  [x]
  (cond
    (< x 0) 0
    (> x 1) 1
    :else (float x))) ; drop clojure's fractional values

(defn fold
  "Fold a value to lie within the range [0-1]."
  [x]
  (mod x 1))

(defn rgb
  "Convert an HSL triplet to RGB.  This uses the algorithm described at
  [Wikipedia](https://en.wikipedia.org/wiki/HSL_and_HSV#From_HSL)."
  [[h s l]]
  (let [h (* 6 (fold h))
        s (clip s)
        l (clip l)
        chroma (* s (- 1 (abs (dec (* 2 l)))))
        x (* chroma (- 1 (abs (dec (mod h 2)))))
        [r g b]
        (cond
          (< h 1) [chroma, x, 0]
          (< h 2) [x, chroma, 0]
          (< h 3) [0, chroma, x]
          (< h 4) [0, x, chroma]
          (< h 5) [x, 0, chroma]
          :else [chroma, 0, x]
          )
        m (- l (/ chroma 2))]
    [(+ r m) (+ g m) (+ b m)]))

(defn hsl
  "Convert an RGB triplet to HSV.  Again, this uses an algorithm described at
  [Wikipedia](https://en.wikipedia.org/wiki/HSL_and_HSV#Formal_derivation)."
  [[r g b]]
  (let [r (clip r)
        g (clip g)
        b (clip b)
        mx (max r g b)
        mn (min r g b)
        chroma (- mx mn)
        h (/ (cond
               (> 1e-6 chroma) 0.0
               (= mx r) (/ (- g b) chroma)
               (= mx g) (+ 2 (/ (- b r) chroma))
               :else (+ 4 (/ (- r g) chroma))
               ) 6)
        l (/ (+ mn mx) 2)
        s
        (if (> 1e-6 chroma)
          0.0
          (/ chroma (- 1 (abs (dec (* 2 l))))))]
    [h s l]))

(defn lighten
  "Scale lightness by the given factor."
  [k [h s l]]
  [h s (clip (* k l))])

(defn rotate
  "Rotate hue by the given amount (a complete rotation requires a change of 1)."
  [k [h s l]]
  [(fold (+ k h)) s l])

;; Some HSL colours for testing, etc.

(def white [0 0 1])
(def black [0 0 0])
(def red (hsl [1 0 0]))
(def green (hsl [0 1 0]))
(def blue (hsl [0 0 1]))
