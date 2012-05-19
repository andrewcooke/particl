(ns cl.parti.main
  (:use clojure.java.io)
  (:use (cl.parti cli mosaic state png hsl utils))
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
        hash (:hash-algorithm options)
        input (:input-type options)]
    (case input
      "hex" (hex-state hash)
      "word" (string-state hash)
      "file" (stream-state hash))))


; generate --------------------------------------------------------------------

(defn make-generate [options]
  (let [n (:tile-number options)
        k (:complexity options)
        render (:render options)]
    (fn [state]
      (let [[mosaic state] (render options state)]
        (expand
          (transform mosaic state))))))


; driver ----------------------------------------------------------------------

(defn http-driver [options]
  (fn [print hash _] nil))

(defn make-file-output [path]
  (fn [index rows]
    (when (and (= index 1) (= -1 (.indexOf path "%d")))
      (printf "WARNING: Multiple output images but no '%%d' in --output\n"))
    (let [path (format path index)
          n (count rows)
          os (output-stream path)
          print (if (< n 17) (print-png-indexed os) (print-png-8-bit os))]
      (print-rows print rows))))

(defn make-gui-output []
  (fn [index image]
    (error "gui-output " index image)))

(defn make-generic-driver [output]
  (fn [generate hash input]
    (doall (map-indexed output (map (comp generate hash) input)))))

(defn make-driver [options]
  (let [scale (:tile-size options)
        border [(:border-colour options) (:border-width options)]]
    (if (:http-bind options)
      (http-driver scale border options)
      (make-generic-driver
        (if (:output options)
          (make-file-output (:output options))
          (make-gui-output))))))


; main ------------------------------------------------------------------------

; input generates a sequence (per file) of sources (unused by http).
; hash turns input into a "state".
; generate creates a mosaic from a state.
; driver drives the process and delivers the results.
(defn -main [& args]
  (let [[args options] (handle-args args)
        driver (make-driver options)
        generate (make-generate options)
        hash (make-hash options)
        input (make-input options args)]
    (driver generate hash input)))
