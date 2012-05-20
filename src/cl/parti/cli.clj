(ns cl.parti.cli
  (:use (cl.parti hsl utils square fourier))
  (:import java.awt.Color)
  (:use [clojure.string :only [replace] :rename {replace str-replace}])
  (:use clojure.tools.cli)
  (:use clojure.math.numeric-tower))


; provide access to mosaic generation for various different tasks (this
; also demonstrates the functionality of the various modules):
; - as a web server, running on a particular port, which returns mosaic
;   images for the configured configuration (size, border, etc).
; - as a command line utility that generates a mosaic for a given input
;   value and configuration.
; - as a command line utility that checksums a file and gives a
;   standard image.

; a "style" is a combination of:
; - [n]umber of tiles
; - [s]ize of each tile (in pixels)
; - [c]olour of background
; - [w]idth of background (in pixels)
; - [k] complexity (by default, inferred from n)
; the program supports several "pre-packaged" styles.  only one can be
; given, and provides the default values for for the parameters above, which
; can be modified individually by providing specific values.

; default behaviour is to display the image generated by hashing the input.
; input can be either a list of files on the command line, or data from stdin.

(defn ?merge [map extra]
  (reduce (fn [map [k v]] (if (map k) map (assoc map k v)))
    map extra))

(defn make-numeric-parser [parse type]
  (fn [value name]
    (try
      (+ 0 value) ; return if numeric
      (catch Exception e
        (try
          (parse value)
          (catch Exception e
            (error name " " value " not " type)))))))

(def parse-int (make-numeric-parser (memfn Integer/parseInt) "an integer"))
(def parse-double (make-numeric-parser (memfn Double/parseDouble) "a float"))

; name can be "--xxx" or :xxx
(defn assert-range [x mn mx name]
  (let [name (str-replace name ":" "--")
        x (parse-int x name)]
    (when (< x mn)
      (error name " below " mn))
    (when (> x mx)
      (error name " over " mx))))

; this runs after style has provided defaults, so we know something is
; defined for the value
(defn pick-component [options name default]
  (if-let [value (name options)]
    (do
      (assert-range value 0 255 name)
      (/ (parse-int value name) 255))
    default))


; option processing, in order -------------------------------------------------

(defn check-tile-number [options]
  (when-let [n (:tile-number options)]
    (assert-range n 4 32 "--tile-number"))
  options)

(defn check-hash-algorithm [options]
  (when-let [input (:input options)]
    (when (and (= input "hex") (:hash-algorithm options))
      (error "--hash-algorithm conflicts with --input hex")))
  options)

; this runs before anything else related to border-colour, so the value
; will be either nil or a colour name.  note that we set RGB here.
(defn lookup-colour [options]
  (if-let [name (:border-colour options)]
    (try
      (let [colour (.get (.getField Color name) nil)
            rgb [(.getRed colour) (.getGreen colour) (.getBlue colour)]]
        (assoc options :border-colour (map #(/ % 255) rgb)))
      (catch Exception e
        (error "--border-colour " name " unsupported")))
    options))

(defn set-http [options]
  (if (some identity
        (map options
          [:http-port :http-bind :http-cache :http-param :http-path ]))
    (do
      (when (:output options) (error "--output conflicts with http use"))
      (?merge options {:http-port 8081 :http-bind "0.0.0.0" :http-cache 100}))
    options))

(defn set-input [options]
  (let [input (:input-type options)
        http (:http-bind options)]
    (case input
      "file" (if http
               (error "--input " input " conflicts with http")
               options)
      nil (assoc options :input-type (if http "word" "file"))
      options)))

; note that this sets colour as rgb as components are over-ridden before
; conversion.
(defn set-style [options]
  (let [input (:input-type options)
        style (:style (?merge options
                        (if (= input "word")
                          {:style "user"}
                          {:style "hash"})))]
    (case style
      "hash" (?merge options
               {:tile-number 20 :tile-size 4
                :border-colour [0 0 0] :border-width 1
                :hash-algorithm "SHA-512" :render "fourier"})
      "user" (?merge options
               {:tile-number 5 :tile-size 20
                :border-colour [1 1 1] :border-width 3
                :hash-algorithm "MD5" :render "fourier"})
      (error "--style " style " unsupported"))))

(defn colour-components [options]
  (let [[r g b] (:border-colour options)
        r (pick-component options :border-red r)
        g (pick-component options :border-green g)
        b (pick-component options :border-blue b)]
    (assoc options :border-colour (hsl [r g b]))))

(defn set-complexity [options]
  (let [options (?merge options {:complexity 1})
        k (:complexity options)]
    (do
      (assert-range k 0.1 10 "--complexity")
      options)))

(defn set-render [options]
  (let [render (:render options)]
    (case render
      "square" (assoc options :render square)
      "fourier" (assoc options :render fourier)
      :else (error "--render " render " unsupported"))))

(defn make-conversion [parser names]
  (fn [options]
    (apply merge (map (fn [[k v]]
                        (if (and v
                              (names k))
                          {k (parser v k)}
                          {k v}))
                   options))))

(def convert-int
  (make-conversion parse-int #{:tile-number :tile-size :border-width :http-port }))

(def convert-double
  (make-conversion parse-double #{:complexity }))

(defn show-options [options]
  (when (:verbose options) (println options))
  options)

(defn handle-args [args]
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
    ["--http-port" "Run an HTTP server on this port"]
    ["--http-bind" "Bind an HTTP server to this address"]
    ["--http-cache" "Number of images to cache"]
    ["--http-param" "The HTTP parameter to be used as input"]
    ["--http-path" "The prefix stripped from the path"]
    ["-k" "--complexity" "The image complexity (float, ~1)"]
    ["-r" "--render" "How image rendered (square, fourier)"]
    ["-a" "--hash-algorithm" "The hash to use (SHA-512, etc)"]
    ["-h" "--help" "Display help" :flag true]
    ["-v" "--verbose" "Additional output" :flag true]
    )]
    (when (:help options)
      (println banner)
      (System/exit 0))
    ; this gives a set of options with style values that are checked,
    ; complete and consistent.  it uses values from the default style and
    ; a complexity that depends on the mosaic size to fill gaps.  colour
    ; components replace those from the style.
    [args (reduce (fn [o f] (f o)) options
      ; ordering below is critical!
      [check-tile-number check-hash-algorithm
       lookup-colour set-http set-input set-style colour-components
       set-complexity set-render
       convert-int convert-double show-options])]))
