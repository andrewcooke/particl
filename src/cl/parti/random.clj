(ns cl.parti.random
  (:use clojure.math.numeric-tower)
  (:import java.security.MessageDigest))


; the secure random signature in java isn't clear on repeatability (the
; docs emphasize non-repeatability), so we use an explicit (sha-512) hash here.
; we care about "cosmetic" randomness (once the hash has been generated) and
; *guaranteed* repeatability, so this should not be a problem.

; we have 512 bits, 64 bytes, of initial state.  we need to generate
; random values between 0 and 1 with a byte's resolution, so have 64 initial
; independent values.  to extend that we will circulate the bytes, adding
; a simple rotation and xor to avoid immediate duplication.

; generate a queue - for some reason not exposed at teh clojure level.
(defn queue [vals]
  (reduce conj clojure.lang.PersistentQueue/EMPTY vals))

; hash text to produce a queue of bytes
(defn state [text]
  (let [hash (. MessageDigest getInstance "SHA-512")
        bytes (.digest hash (.getBytes text))]
    (queue bytes)))

; rotate b by n bits
(defn rotate-byte [n b]
  (let [b (if (< b 0) (+ b 0x100) b)
        left (bit-shift-left (bit-and b (dec (expt 2 n))) (- 8 n))
        right (bit-shift-right b n)]
    (unchecked-byte (bit-or left right))))

; an ad-hoc scrambling (flip alternate bits and rotate 3 bytes) used to
; recycle state and extend the "random" stream of values.
(defn scramble-byte [b]
  (unchecked-byte (bit-xor 0x55 (rotate-byte 3 b))))

; given a queue of bytes, generate a lazy sequence that cycles through the
; queue, using scramble-byte to extend the period.
(defn byte-stream [queue]
  (lazy-seq
    (let [old (peek queue)
          new (scramble-byte old)]
      (cons old (byte-stream (conj (pop queue) new))))))

; package all the above into a single function
(defn hash-state [text]
  (byte-stream (state text)))


; the following extract a "random" value from the stream, returning the
; value and a new stream.

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

; 1 or -1

(defn- z-neg [x]
  (if (= x 0) -1 x))

(defn sign [state]
  (let [[b state] (unbiased-byte state)]
    [(z-neg b) state]))

(defn sign-2 [state]
  (let [[b state] (whole-byte state)]
    [(z-neg (bit-and b 1)) (z-neg (/ (bit-and b 2) 2)) state]))
