(ns cl.parti.random
  (:import javax.crypto.Cipher)
  (:import javax.crypto.spec.SecretKeySpec)
  (:import javax.crypto.spec.IvParameterSpec))


; we need a random number generator that can be seeded by a large
; number of bits, is sensitive to even small changes in the seed,
; and is robust to lack of bites in the initial seed (eg if all
; zeroes).  obvious solution is a block cipher in counter mode,
; but they only seem to go to 128 bit keys in standard java, so
; use several in parallel and xor the output.  block size is 128
; bits (16 bytes).  final result is a sequence of bytes.

(def BLOCK_SIZE 16)
(def KEY_SIZE 16)
(def BLANK (byte-array BLOCK_SIZE (byte 0)))

(defn- stream-aes [buffer offset cipher]
  (lazy-seq
    (if (< offset (count buffer))
      (cons (nth buffer offset) (stream-aes buffer (inc offset) cipher))
      (stream-aes (.update cipher BLANK) 0 cipher))))

(defn- pad-key [key offset]
  (let [zero (* offset KEY_SIZE)]
    (byte-array (for [i (range KEY_SIZE)]
                  (byte (nth key (+ zero i) 0))))))

; offset is in units of key size, but may be called with too few
; bytes remaining (if the key is not a multiple of the key size).
; in that case, zero pad
(defn- init-aes [key offset]
  (let [c (Cipher/getInstance "AES/CTR/NoPadding")
        k (SecretKeySpec. (pad-key key offset) "AES")
        iv (byte-array BLOCK_SIZE (byte offset))]
    (do (.init c Cipher/ENCRYPT_MODE k (IvParameterSpec. iv))
      (stream-aes [] 0 c))))

(defn- parallel [streams]
  (lazy-seq
    (let [b (map first streams)
          streams (map rest streams)]
      (cons (apply bit-xor b) (parallel streams)))))

(defn random [hash]
  (assert (> (count hash) 0) "No data to seed random stream")
  (let [n (int (/ (+ (dec KEY_SIZE) (count hash)) KEY_SIZE))]
    (if (= 1 n)
      (init-aes hash 0)
      (parallel (for [i (range n)] (init-aes hash i))))))


; various helpers that take the seq of bytes and return a useful
; value plus a new sequence.

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
  (let [b (first stream)
        stream (rest stream)]
    [(/ (+ 128 b) 256.0) stream]))

; [0.0 1.0]
(defn uniform-closed [stream]
  (let [b (first stream)
        stream (rest stream)]
    [(/ (+ 128 b) 255.0) stream]))

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
  (let [[b state] (whole-byte state)]
    [(z-neg (bit-and b 1)) state]))

(defn sign-2 [state]
  (let [[b state] (whole-byte state)]
    [(z-neg (bit-and b 1)) (z-neg (/ (bit-and b 2) 2)) state]))
