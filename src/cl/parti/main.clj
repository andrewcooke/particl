(ns cl.parti.main
  (:use clojure.java.io)
  (:use (cl.parti cli mosaic png hsl utils random))
  (:gen-class ))


(def LIGHTNESS 0.75) ; relative strength of l changes, relative to h


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
  (let [hash (:hash-algorithm options)
        input (:input-type options)]
    (input hash)))


; generate --------------------------------------------------------------------

(defn make-generate [options]
  (let [render (:render options)]
    (fn [state]
      (let [rows (render options state)
            [h-v-l state] (sign state)
            [hue state] (uniform-open state)]
        (floats-to-hsl options LIGHTNESS h-v-l hue rows)))))


; driver ----------------------------------------------------------------------

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

(defn make-driver [options]
  (let [output (if-let [out (:output options)] (make-file-output out) (make-gui-output))]
    (fn [generate hash input]
      (doall (map-indexed output (map (comp generate hash) input))))))


; main ------------------------------------------------------------------------

; input generates a sequence (per file).
; hash turns input into a "state".
; generate creates a mosaic from a state.
; driver drives the process and delivers the results.

(defn particl [options args]
  (let [input (make-input options args)
        hash (make-hash options)
        generate (make-generate options)
        driver (make-driver options)]
    (driver generate hash input)))

(defn -main [& args]
  (apply particl (handle-args args)))
