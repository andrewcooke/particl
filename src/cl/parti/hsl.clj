(ns cl.parti.hsl
  (:use (cl.parti random))
  (:use clojure.math.numeric-tower))

(defn clip [x]
  (cond
    (< x 0) 0
    (> x 1) 1
    :else x))

(defn fold [x]
  (mod x 1))

; https://en.wikipedia.org/wiki/HSL_and_HSV#From_HSL
(defn rgb [[h s l]]
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

; https://en.wikipedia.org/wiki/HSL_and_HSV#Formal_derivation
(defn hsl [[r g b]]
  (let [r (clip r)
        g (clip g)
        b (clip b)
        mx (max r g b)
        mn (min r g b)
        chroma (- mx mn)
        h (/ (cond
               (= 0 chroma) 0
               (= mx r) (/ (- g b) chroma)
               (= mx g) (+ 2 (/ (- b r) chroma))
               :else (+ 4 (/ (- r g) chroma))
               ) 6)
        l (/ (+ mn mx) 2)
        s
        (if (= 0 chroma)
          0
          (/ chroma (- 1 (abs (dec (* 2 l))))))]
    [h s l]))

(defn lighten [k [h s l]]
  [h s (clip (* k l))])

(defn rotate [k [h s l]]
  [(fold (+ k h)) s l])

; correlate lightness and colour shifts
(defn colourblind [state]
  (let [[k state] (range-closed 8 state)
        [x state] (range-closed 0.1 state)]
    [#(lighten k (rotate k %)) state]))

(def white [0. 0. 1.])
(def black [0. 0. 0.])
(def red (hsl [1. 0. 0.]))
(def green (hsl [0. 1. 0.]))
(def blue (hsl [0. 0. 1.]))
