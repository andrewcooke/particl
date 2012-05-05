(ns cl.parti.main
  (:use clojure.java.io)
  (:use (cl.parti cli mosaic random png hsl utils))
  (:gen-class ))


; input -----------------------------------------------------------------------

(defn lazy-open [file]
  (defn helper [rdr]
    (lazy-seq
      (if-let [line (.readLine rdr)]
        (cons line (helper rdr))
        (do (.close rdr) nil))))
  (lazy-seq
    (helper (clojure.java.io/reader file))))

(defn args-input [options args]
  (if (= "file" (:input options))
    (map lazy-open args)
    (map list args)))

(defn stdin-input [options]
  (error "stdin-input " options))

(defn make-input [options args]
  (if args
    (args-input options args)
    (stdin-input options)))


; hash ------------------------------------------------------------------------

(defn make-hash [options]
  (let [join (partial apply str)]
    (if (= "hex" (:input options))
      (comp hash-bytes join)
      (comp hash-state join))))


; print ----------------------------------------------------------------------

(defn make-print [options]
  (let [n (:tile-number options)
        k (:complexity options)]
    (fn [state]
      (let [[mosaic state] (random-mosaic n state)
            transform (make-colourblind (mosaic 2))]
        (repeated-transform mosaic k transform state)))))


; driver ----------------------------------------------------------------------

(defn http-driver [options]
  (fn [print hash _] nil))

(defn make-file-output [scale border path]
  (fn [index image]
    (when (and (= index 1) (= -1 (.indexOf path "%d")))
      (printf "WARNING: Multiple output images but no '%%d' in --output\n"))
    (let [path (format path index)
          print (print-png-indexed (output-stream path))]
      (print-mosaic print image scale border))))

(defn make-gui-output [options]
  (fn [index image]
    (error "gui-output " index image)))

(defn make-generic-driver [output]
  (fn [print hash input]
    (doall (map-indexed output (map (comp print hash) input)))))

(defn make-driver [options]
  (let [scale (:tile-size options)
        border [(:border-colour options) (:border-width options)]]
    (if (:http-bind options)
      (http-driver scale border options)
      (make-generic-driver
        (if (:output options)
          (make-file-output scale border (:output options))
          (make-gui-output scale border))))))


; main ------------------------------------------------------------------------

; input generates a sequence (per file) of a sequence (per line) of strings
; except for http, where it is nil.
; hash turns a sequence of strings into a "state".
; print generates a mosaic from a state.
; driver drives the process and delivers the results.
(defn -main [& args]
  (let [[args options] (handle-args args)
        driver (make-driver options)
        print (make-print options)
        hash (make-hash options)
        input (make-input options args)]
    (driver print hash input)))
