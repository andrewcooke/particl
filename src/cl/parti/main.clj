(ns ^{:doc "

Assemble the pipeline components to generate mosaics.

"
      :author "andrew@acooke.org"}
  cl.parti.main
  (:use clojure.java.io)
  (:use (cl.parti cli))
  (:gen-class ))


(defn particl
  "Generate a pipeline from the following components:

  - *reader* extracts values from command line args or stdin and then applies
    the hash;
  - *hash* hashes the values;
  - *builder* constructs the mosaic, in an internal format, from the hash;
  - *normalize* reduces the range of the internal format;
  - *render* converts the internal format to an array of HSL pixels;
  - *editor* modifies the HSL pixels;
  - *display* presents the image to the user.

The hash is applied within the reader so that the reader can close streams
after processing.  This allows large inputs to be hashed without holding
the entire value in memory, and without exposing open streams.

The types of return values could vary, as long as the different components
interact correctly, but are currently consistent for each stage."
  [reader hash builder normalize editor render display]
  (fn [args]
    (doseq [state (reader hash args)]
      (-> state builder normalize editor render display))))

(defn -main
  "Combine the pipeline above with the command line option handling in
  `cl.parti.cli` to give a command-line application."
  [& args]
  (let [[[[reader hash] [builder normalize] [editor render] display] args]
        (handle-args args)]
    ((particl reader hash builder normalize editor render display) args)))
