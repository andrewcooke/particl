(ns cl.parti.cli
  (:use (cl.parti hsl utils square fourier state))
  (:import java.awt.Color)
  (:use [clojure.string :only [replace] :rename {replace str-replace}])
  (:use clojure.tools.cli)
  (:use clojure.math.numeric-tower))


(defn printable [name]
  (str-replace name ":" "--"))

(defn parse-int [value name]
  (try
    (int value) ; return if numeric
    (catch Exception e
      (try
        (Integer/parseInt value)
        (catch Exception e
          (error (printable name) " " value " not an integer"))))))

(defn assert-range [x mn mx name]
  (let [x (parse-int x name)]
    (when (< x mn)
      (error (printable name) " below " mn))
    (when (> x mx)
      (error (printable name) " above " mx))))

(defn pick-component [options name default]
  (if-let [value (name options)]
    (do
      (assert-range value 0 255 name)
      (/ (parse-int value name) 255))
    default))

(defmacro defn-defaults [[name option] & cases]
  `(defn ~name [options#]
     (?merge options#
       (let [value# (~option options#)]
         (case value#
           ~@cases
           (error (printable ~option) " " value# " unsupported"))))))

(defmacro defn-translate [[name option] & cases]
  `(defn ~name [options#]
     (assoc options# ~option
       (let [value# (~option options#)]
         (case value#
           ~@cases
           (error (printable ~option) " " value# " unsupported"))))))


; option processing, in order -------------------------------------------------

; validation

(defn convert-int [options]
  (let [names #{:tile-number :tile-size :border-width }]
    (apply merge
      (for [[k v] options]
        {k (if (and v (names k)) (parse-int v k) v)}))))

(defn check-tile-number [options]
  (when-let [n (:tile-number options)]
    (assert-range n 4 32 "--tile-number"))
  options)

(defn check-hash-algorithm [options]
  (when-let [input (:input options)]
    (when (and (= input "hex") (:hash-algorithm options))
      (error "--hash-algorithm conflicts with --input hex")))
  options)

; defaults

(defn-defaults [set-style-1 :input-type ]
  "word" {:style "user"}
  :else {:style "hash"})

(defn-defaults [set-style-2 :style ]
  "hash" {:tile-number 20 :tile-size 4
          :border-colour "black" :border-width 1
          :hash-algorithm "SHA-512" :render "square"}
  "user" {:tile-number 5 :tile-size 20
          :border-colour "white" :border-width 3
          :hash-algorithm "MD5" :render "square"})

; background colour

(defn lookup-colour [options]
  (if-let [name (:border-colour options)]
    (try
      (let [colour (.get (.getField Color name) nil)
            rgb [(.getRed colour) (.getGreen colour) (.getBlue colour)]]
        (assoc options :border-colour (map #(/ % 255) rgb)))
      (catch Exception e
        (error "--border-colour " name " unsupported")))
    options))

(defn colour-components [options]
  (let [[r g b] (:border-colour options)
        r (pick-component options :border-red r)
        g (pick-component options :border-green g)
        b (pick-component options :border-blue b)]
    (assoc options :border-colour (hsl [r g b]))))

; map options to implementing functions

(defn-translate [set-render :render ]
  "square" square
  "fourier" fourier)

(defn-translate [set-input :input-type ]
  "hex" hex-input
  "word" word-input
  "file" file-input)

; other

(defn show-options [options]
  (when (:verbose options) (println options))
  options)


(defn process-options [options]
  (reduce (fn [o f] (f o)) options
    ; ordering below is critical!
    [convert-int check-tile-number check-hash-algorithm
     set-style-1 set-style-2
     lookup-colour colour-components
     set-render set-input
     show-options]))

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
    ["-r" "--render" "How image rendered (square, fourier)"]
    ["-m" "--monochrome" "Greyscale images" :flag true]
    ["-a" "--hash-algorithm" "The hash to use (SHA-512, etc)"]
    ["-h" "--help" "Display help" :flag true]
    ["-v" "--verbose" "Additional output" :flag true])]
    (when (:help options)
      (println banner)
      (System/exit 0))
    [(process-options options) args]))
