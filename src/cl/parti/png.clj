(ns ^{:doc "

A simple interface to the excellent pngj library.

"
      :author "andrew@acooke.org"}
  cl.parti.png
  (:use (cl.parti hsl))
  (:use clojure.math.numeric-tower)
  (:import ar.com.hjg.pngj.PngWriter)
  (:import ar.com.hjg.pngj.ImageInfo)
  (:import ar.com.hjg.pngj.ImageLine)
  (:import ar.com.hjg.pngj.ImageLineHelper)
  (:import ar.com.hjg.pngj.chunks.PngChunkPLTE)
  )


(defn hsl-to-8-bit
  "Convert an HSL triplet of floats to an RGB triplet of unsigned bytes."
  [hsl]
  (map (fn [x] (int (* 255 x))) (rgb hsl)))

(defn print-png-8-bit
  "Write a 2D nested sequence of HSL triplets to an 8-bit PNG file.

  This matches the API expected by `cl.parti.mosaic.print-rows`.  It
  returns two functions; the first prints the data converted by the first."
  [os]
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

(defn print-png-indexed
  "Write a 2D nested sequence of HSL triplets to an indexed PNG file.
  This should be more compact than the equivalent 8-bit file, but is
  restricted to a maximum of 256 distinct colours.  The index is 8-bit
  (in theory we could use smaller indices for small mosaics).

  This matches the API expected by `cl.parti.mosaic.print-rows`.  It
  returns two functions; the first prints the data converted by the first."
  [os]
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
