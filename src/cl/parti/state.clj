(ns cl.parti.state
  (:use (cl.parti utils random))
  (:use clojure.math.numeric-tower)
  (:import java.security.MessageDigest)
  (:import org.apache.commons.codec.binary.Hex))


(def HEX (Hex.))

; hash text to produce a queue of bytes
(defn word-input [hash]
  (fn [text]
    (let [hash (. MessageDigest getInstance hash)]
      (random (.digest hash (.getBytes text))))))

; hash stream of text to produce a queue of bytes
(defn file-input [hash]
  (fn [stream]
    (let [hash (. MessageDigest getInstance hash)
          buffer-size 65535
          buffer (byte-array buffer-size)]
      (defn copy-stream []
        (let [n (.read stream buffer 0 buffer-size)]
          (if (not= n -1)
            (do (.update hash buffer 0 n) (recur))
            (do (.close stream) hash))))
      (random (.digest (copy-stream))))))

(defn hex-input [hash]
  (fn [hex]
    (try
      (random (.decode HEX hex))
      (catch Exception e
        (error hex " is invalid hex: " (.getMessage e))))))
