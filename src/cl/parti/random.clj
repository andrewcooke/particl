(ns cl.parti.random
  (:use (cl.parti utils))
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


; [-128 127]
(defn rand-signed-byte [state]
  (let [[r & state] state]
    [r state]))

; [0 255]
(defn rand-unsigned-byte [state]
  (let [[r state] (rand-signed-byte state)]
    [(unsign-byte r) state]))

; [0 n)
(defn rand-real [n state]
  (let [[r state] (rand-unsigned-byte state)]
    [(* n (/ r 256.0)) state]))

(defn- bitmask
  ([n] (if (< n 2) n (bitmask (bit-shift-right n 1) 1)))
  ([n m] (if (= 0 n) m (recur (bit-shift-right n 1) (inc (* 2 m))))))

; [0 n) (matches rand-int)
(defn rand-byte [n state]
  (assert (> n 0))
  (assert (< n 257))
  (let [[r state] (rand-unsigned-byte state)
        m (bitmask (dec n))
        r (bit-and r m)]
    (if (< r n) [r state] (recur n state))))

; -1 or 1
(defn rand-sign [state]
  (let [[r state] (rand-byte 2 state)]
    [(if (= 1 r) r -1) state]))

