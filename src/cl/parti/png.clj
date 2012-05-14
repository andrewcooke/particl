(ns cl.parti.png
  (:use (cl.parti hsl))
  (:use clojure.math.numeric-tower)
  (:import ar.com.hjg.pngj.PngWriter)
  (:import ar.com.hjg.pngj.ImageInfo)
  (:import ar.com.hjg.pngj.ImageLine)
  (:import ar.com.hjg.pngj.ImageLineHelper)
  (:import ar.com.hjg.pngj.chunks.PngChunkPLTE)
  )


; match pngj to the print-mosaic function.

; in the routines below (which take os and return two functions:
; - os is the output stream for the file
; - size is the number of pixels on a square edge
; - rows is nested lists of rgb colours
;   so rows = [row]; row = [col]; col = [r g b]
; - hsl is a hsl colour triplet

(defn hsl-to-8-bit [hsl]
  (map (fn [x] (int (* 255 x))) (rgb hsl)))

; a simple, 8 bit file, with one value per pixel.
(defn print-png-8-bit [os]
  [(fn [size rows]
     ; 8 bits, no alpha, no grayscale, not indexed
     (let [info (ImageInfo. size size 8 Boolean/FALSE Boolean/FALSE Boolean/FALSE)
           writer (PngWriter. os info)
           line (ImageLine. info)]
       (doseq [[i row] (map-indexed vector rows)]
         (doseq [[j [r g b]] (map-indexed vector row)]
           (ImageLineHelper/setPixelRGB8 line j r g b))
         (.writeRow writer line i))
       (.end writer)))
   hsl-to-8-bit])

; an indexed file.  mosaics have many identical pixels, so this should be
; significantly more compact.
; we still use 8 bits - no attempt is made to pack bits for small mosics.
(defn print-png-indexed [os]
  (let [colours (atom {})]
    [(fn [size rows]
       ; 8 bits, no alpha, no grayscale, indexed
       (let [info (ImageInfo. size size 8 Boolean/FALSE Boolean/FALSE Boolean/TRUE)
             writer (PngWriter. os info)
             line (ImageLine. info)
             palette (PngChunkPLTE. info)]
         (.setNentries palette (count @colours))
         (doseq [rgb (keys @colours)]
           (let [[r g b] rgb]
             (.setEntry palette (@colours rgb) r g b)))
         (.queueChunk (.getMetadata writer) palette)
         (doseq [[i row] (map-indexed vector rows)]
           (.setScanLine line (int-array row))
           (.writeRow writer line i))
         (.end writer)))
     (fn [hsl]
       (let [rgb (hsl-to-8-bit hsl)]
         (when (nil? (@colours rgb))
           (swap! colours #(assoc % rgb (count %))))
         (@colours rgb)))]))
