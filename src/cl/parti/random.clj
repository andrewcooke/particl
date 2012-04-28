(ns cl.parti.random
  (:use clojure.math.numeric-tower)
  (:import java.security.MessageDigest))

; the secure random signature in java isn't clear on repeatability, so
; we use an explicit (sha-512) hash here.  we care about "cosmetic"
; randomness (once the hash has been generated), but guaranteed
; repeatability.

; we have 512 bits, 64 bytes, of initial state.  we need to generate
; random values between 0 and 1 with a byte's resolution, so have 64 initial
; independent values.  to extend that we will circulate the bytes, adding
; a simple rotation and xor to avoid immediate duplication.

(defn queue [vals]
  (reduce conj clojure.lang.PersistentQueue/EMPTY vals))

(defn state [text]
  (let [hash (. MessageDigest getInstance "SHA-512")
        bytes (.digest hash (.getBytes text))]
    (queue bytes)))

(defn rotate-byte [n b]
  (let [b (if (< b 0) (+ b 0x100) b)
        left (bit-shift-left (bit-and b (dec (expt 2 n))) (- 8 n))
        right (bit-shift-right b n)]
    (unchecked-byte (bit-or left right))))

(defn scramble-byte [b]
  (unchecked-byte (bit-xor 0x55 (rotate-byte 3 b))))

(defn byte-stream [s]
  (lazy-seq
    (let [old (peek s)
          new (scramble-byte old)]
      (cons old (byte-stream (conj (pop s) new))))))

; [-127 127]
(defn unbiased-byte [stream]
  (let [b (first stream)
        stream (rest stream)]
    (if (= -128 b) (recur stream) [b stream])))

; [0 255]
(defn whole-byte [stream]
  [(+ 128 (first stream)) (rest stream)])

; [0.0 1.0)
(defn uniform-open [stream]
  [(/ (+ 128 (first stream)) 256.0) (rest stream)])

; [0.0 1.0]
(defn uniform-closed [stream]
  [(/ (+ 128 (first stream)) 255.0) (rest stream)])

; [lo hi]
(defn range-closed
  ([hi stream] (range-closed (- hi) hi stream))
  ([lo hi stream]
  (let [[x stream] (uniform-closed stream)]
    [(+ lo (* x (- hi lo))) stream])))
