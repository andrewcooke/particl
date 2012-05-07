(ns cl.parti.main
  (:use clojure.java.io)
  (:use (cl.parti cli mosaic random png hsl utils))
  (:gen-class ))


; input -----------------------------------------------------------------------

(defn args-input [options args]
  (if (= "file" (:input-type options))
    (map input-stream args)
    args))

(defn stdin-input [options]
  (error "stdin-input " options))

(defn make-input [options args]
  (if args
    (args-input options args)
    (stdin-input options)))


; hash ------------------------------------------------------------------------

(defn make-hash [options]
  (let [join (partial apply str)
        input (:input-type options)]
    (case input
      "hex" hex-state
      "word" string-state
      "file" stream-state)))


; print ----------------------------------------------------------------------

(defn make-print [options]
  (let [n (:tile-number options)
        k (:complexity options)]
    (fn [state]
      (let [[mosaic state] (random-mosaic n state)
            transform (make-colourblind (mosaic 0) (mosaic 2))]
        (repeated-transform mosaic k transform state)))))


; driver ----------------------------------------------------------------------

(defn http-driver [options]
  (fn [print hash _] nil))

(defn make-file-output [scale border path]
  (fn [index image]
    (when (and (= index 1) (= -1 (.indexOf path "%d")))
      (printf "WARNING: Multiple output images but no '%%d' in --output\n"))
    (let [path (format path index)
          n (image 0)
          os (output-stream path)
          print (if (< n 17) (print-png-indexed os) (print-png-8-bit os))]
      (print-mosaic print image scale border))))

(defn make-gui-output [scale border]
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

; input generates a sequence (per file) of sources (unused by http).
; hash turns input into a "state".
; print generates a mosaic from a state.
; driver drives the process and delivers the results.
(defn -main [& args]
  (let [[args options] (handle-args args)
        driver (make-driver options)
        print (make-print options)
        hash (make-hash options)
        input (make-input options args)]
    (driver print hash input)))
