(ns cl.parti.png
  (:use (cl.parti hsl))
  (:use clojure.math.numeric-tower)
  (:import ar.com.hjg.pngj.PngWriter)
  (:import ar.com.hjg.pngj.ImageInfo)
  (:import ar.com.hjg.pngj.ImageLine)
  (:import ar.com.hjg.pngj.ImageLineHelper))


; size is the number of pixels on a square edge
; colours is nested lists of rgb colours
; so colours = [row]; row = [col]; col = [r g b]
(defn make-print-png [os]
  (fn [size colours]
    ; 8 bits, no alpha, no grayscale, not indexed
    (let [info (ImageInfo. size size 8 Boolean/FALSE Boolean/FALSE Boolean/FALSE)
          writer (PngWriter. os info)
          line (ImageLine. info)]
      (doseq [[i row] (map-indexed vector colours)]
        (doseq [[j [r g b]] (map-indexed vector row)]
          (println size i j r g b)
          (ImageLineHelper/setPixelRGB8 line j r g b))
        (.writeRow writer line i))
      (.end writer))))

(defn png-8-bit [hsl]
  (map (fn [x] (int (* 255 x))) (rgb hsl)))
