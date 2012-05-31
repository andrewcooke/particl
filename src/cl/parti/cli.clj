(ns ^{:doc "

This module interacts with the user on the command line.  It provides help,
parses options, and assembles the pipeline components necessary to generate
the mosaic(s).

"
      :author "andrew@acooke.org"}
  cl.parti.cli
  (:use (cl.parti hsl utils diagonal fourier input output))
  (:import java.awt.Color)
  (:use [clojure.string :only [replace] :rename {replace str-replace}])
  (:use clojure.tools.cli)
  (:use clojure.math.numeric-tower))

;; #### Utility for keyword conversion

(defn option-name
  "Internally, options are stored in a map from keywords to values.  These
  keywords match the command line options, except that they start with a
  colon rather than '--'.
  
  This routine, when given a keyword or option name, returns an option
  name (including '--') suitable for displaying to the user."
  [name]
  (str-replace name ":" "--"))

;; #### A couple of utilities for handling numeric options

(defn parse-int
  "Convert a value to an integer.  On failure, generate a useful error."
  [value name]
  (try
    (int value) ; return if numeric
    (catch Exception e
      (try
        (Integer/parseInt value)
        (catch Exception e
          (error (option-name name) " " value " not an integer"))))))

(defn assert-range
  "Check whether a value lies within some range.  On failure, generate a useful
   error."
  [x mn mx name]
  (let [x (parse-int x name)]
    (when (< x mn)
      (error (option-name name) " below " mn))
    (when (> x mx)
      (error (option-name name) " above " mx))))

;; #### Macros to handle two common cases

(defmacro key-case
  "A case statement that uses the value associated with `key` in the options.
  If there is no match then generate a useful error."
  [[key options] & cases]
  `(let [value# (~key ~options)]
     (case value#
       ~@cases
       (error (option-name ~key) " " value# " unsupported"))))

(defmacro defn-defaults
  "Using the same mechanism as `key-case` (ie selecting on option values),
  define a function that provides default values for other options.

  The function returns a new option map, merged with the selected case.
  If no case matches then (you guessed) generate a useful error."
  [[name key] & cases]
  `(defn ~name
     [options#]
     (?merge options#
       (key-case [~key options#]
         ~@cases))))

;; ## Option processing
;;
;; The following routines are applied, in order, to the raw options received
;; from the user.  The modifications accumulate, adding defaults and inferring
;; related values, until we have enough information to construct the pipeline.

(defn convert-int
  "Convert all numerical options to integers."
  [options]
  (let [names #{:tile-number :tile-size :border-width :raw}]
    (apply merge
      (for [[k v] options]
        {k (if (and v (names k)) (parse-int v k) v)}))))

(defn check-tile-number
  "Validate that the number of tiles along one side of a mosaic lies within
  a reasonable range."
  [options]
  (when-let [n (:tile-number options)]
    (assert-range n 4 32 "--tile-number"))
  options)

(defn check-hash-algorithm
  "The 'hex' input type implies that a hash has already been calculated.
  If that is chosen then we check that the user does not also specify a
  hash algorithm - doing so suggests confusion and insecure use."
  [options]
  (when-let [input (:input options)]
    (when (and (= input "hex") (:hash-algorithm options))
      (error "--hash-algorithm conflicts with --input hex")))
  options)

;; Provide a default style, based on the input type.  If we are hashing
;; individual words then we select the 'user' style - a simple, attractive
;; design, suitable for identifying users.  Otherwise (ie when hashing files)
;; we select the 'hash' style, which is more secure (contains more detail).
(defn-defaults [set-style-1 :input-type ]
  "word" {:style "user"}
  :else {:style "hash"})

;; Expand the given style to affect other, appropriate options.  If a user
;; has specified an option on the command line then that will take precedence
;; over the defaults given here.
(defn-defaults [set-style-2 :style ]
  "hash" {:tile-number 20 :tile-size 4
          :border-colour "black" :border-width 1
          :hash-algorithm "SHA-512" 
          :builder "rectangle" :normalize "histogram"}
  "user" {:tile-number 5 :tile-size 20
          :border-colour "white" :border-width 3
          :hash-algorithm "MD5" 
          :builder "rectangle" :normalize "sigmoid"})

;; #### Combine multiple options to find the background colour

(defn lookup-colour
  "Extract a 'border-colour' option, which should be a common colour name,
  and convert it to an RGB value use the names of static fields in Java's
  Color class."
  [options]
  (if-let [name (:border-colour options)]
    (try
      (let [colour (.get (.getField Color name) nil)
            rgb [(.getRed colour) (.getGreen colour) (.getBlue colour)]]
        (assoc options :border-colour (map #(/ % 255) rgb)))
      (catch Exception e
        (error "--border-colour " name " unsupported")))
    options))

(defn pick-component
  "A utility to extract the option for a single (R, G or B) component
  of the background."
  [options name default]
  (if-let [value (name options)]
    (do
      (assert-range value 0 255 name)
      (/ (parse-int value name) 255))
    default))

(defn colour-components
  "Take the existing RGB background (defined by 'background-colour', which
  has a default provided via a style, and so always exists) and replace any
  components that were given explicitly."
  [options]
  (let [[r g b] (:border-colour options)
        r (pick-component options :border-red r)
        g (pick-component options :border-green g)
        b (pick-component options :border-blue b)]
    (assoc options :border-colour (hsl [r g b]))))

;; #### Remaining option processing

(defn show-options
  "The 'verbose' option dumps the map of options to stdout.  This is useful
  when debugging, or to see what different styles provide."
  [options]
  (when (:verbose options) (println options))
  options)

(defn process-options
  "Assemble all the functions above as a single pipeline to populate
  and extend the options."
  [options]
  (-> options
    convert-int check-tile-number check-hash-algorithm
    set-style-1 set-style-2
    lookup-colour colour-components
    show-options))

;; ## Pipeline construction

;; Select the appropriate functions from other modules, based on the
;; options constructed above, so that we can construct the mosaic pipeline.

(defn select-input
  "The 'reader' function is a source of input data, using either command line
  arguments or stdin; the 'hash' function hashes this data.  These are
  selected together, based on the 'input-type' option and the presence of
  command line arguments."
  [options hash args]
  (key-case [:input-type options]
    "hex" [(if args literal-reader line-reader) (hex-hash hash)]
    "word" [(if args literal-reader line-reader) (word-hash hash)]
    "file" [(if args file-reader stdin-reader) (stream-hash hash)]))

(defn select-normalize
  [options]
   (key-case [:normalize options]
     "sigmoid" normalize-sigmoid
     "histogram" normalize-histogram))

(defn select-builder
  "The 'builder' function constructs an abstract, internal representation of
  the image, given the hash; the 'render' function expands that to HSL pixels.
  Their selection is based on multiple command-line options."
  [options n]
  (let [scale (:tile-size options)
        colour (:border-colour options)
        width (:border-width options)
        mono (:monochrome options)
        raw (:raw options)
        render (comp (corners n scale width) (render-floats n scale colour width mono raw))
        norm (select-normalize options)]
    (key-case [:builder options]
      "rectangle" [(rectangle n) norm render]
      "square" [(square n) norm render]
      "fourier" [(fourier n) norm render])))

(defn select-display
  "The 'display' function presents the HSL pixels to the user as a concrete
  image.  If the user defines the 'output' option then this is saved to a
  file; otherwise it is displayed on the screen."
  [options]
  (if-let [path (:output options)]
    (file-display path)
    (gui-display)))

(defn select-functions [options args]
  "Combine the slections above to identify all pipeline components."
  (let [n (:tile-number options)
        hash (:hash-algorithm options)]
    [(select-input options hash args)
     (select-builder options n)
     (select-display options)]))


;; ## Command line interface

(defn handle-args
  "Define the available command-line keywords, guide the the user, process the
  entered options, and select the pipeline components necessary to create
  mosaics."
  [args]
  (let [[options args banner] (cli args
    ["-s" "--style" "A predefined style (hash, user)" :default "hash"]
    ["-i" "--input-type" "How to interpret input (file, hex, word)"]
    ["-o" "--output" "File to which an image will be written"]
    ["-n" "--tile-number" "Number of tiles"]
    ["-p" "--tile-size" "Number of pixels per tile"]
    ["-c" "--border-colour" "Border colour (white, black, etc)"]
    ["-w" "--border-width" "Border width in pixels"]
    ["--border-red" "Border red component (0-255)"]
    ["--border-green" "Border green component (0-255)"]
    ["--border-blue" "Border blue component (0-255)"]
    ["--builder" "How image is built (rectangle, square, fourier)"]
    ["--normalize" "Image normalisation (histogram, sigmoid)"]
    ["--monochrome" "Greyscale images" :flag true]
    ["--raw" "Basic format, fixed hue (0-255)"]
    ["-a" "--hash-algorithm" "The hash to use (SHA-512, etc)"]
    ["-h" "--help" "Display help" :flag true]
    ["-v" "--verbose" "Additional output" :flag true])]
    (when (:help options)
      (println banner)
      (System/exit 0))
    [(-> options process-options (select-functions args)) args]))
