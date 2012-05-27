(ns ^{:doc "

The render functions generate a graphical mosaic from a sequence of random
values.  The source of these values must meet several conditions:

- it can be seeded by a large number of bits;
- it is sensitive to even small changes in the seed;
- it is robust to a lack of bits in the initial seed (eg if all zeroes).

I searched for a standard solution to this problem and found that a block
cipher in counter mode should be appropriate.  However, standard Java only
supports block ciphers up to 128 bit keys.  To extend the key space multiple
ciphers are run in parallel; their results are combined using xor.

*IMPORTANT* - The approach and code here need review by someone more competent
than me.

"
      :author "andrew@acooke.org"}
  cl.parti.random
  (:use (cl.parti utils))
  (:import javax.crypto.Cipher)
  (:import javax.crypto.spec.SecretKeySpec)
  (:import javax.crypto.spec.IvParameterSpec)
  (:import java.security.MessageDigest))


;; Constants for AES in counter mode.

(def ^:private CIPHER "AES")
(def ^:private CIPHER_SPEC (str CIPHER "/CTR/NoPadding"))
(def ^:private BLOCK_SIZE 16)
(def ^:private NONCE_SIZE 4)
(def ^:private IV_SIZE 8)
(def ^:private CTR_SIZE 4)
(def ^:private KEY_SIZE 16)

(def ^:private ^{:doc "The hash used for nonce and IV."} HASH "SHA-1")


;; ## Basic counter mode operation
;;
;; The general approach follows
;; [RFC3686](http://www.faqs.org/rfcs/rfc3686.html), except that Java
;; handles the increment of the counter.

(def ^:private ^{:doc "An array of zeroes; used as the 'plaintext' since we
want to access the key stream."}
  BLANK (byte-array BLOCK_SIZE (byte 0)))

(defn- stream-blocks
  "Run the given cipher, generating a lazy stream of blocks.  The underlying
  Java code increments the counter after each loop, generating a lazy stream
  of blocks."
  [cipher]
  (lazy-seq
    (let [block (.update cipher BLANK)]
      (cons block (stream-blocks cipher)))))

(defn- stream-bytes
  "Convert a stream of blocks to a stream of bytes.

  The first form re-calls with the head block and a zero offset.

  The second form recurses through the available bytes in the block and
  then re-calls with the remaining blocks."
  ([blocks] (stream-bytes (first blocks) 0 (rest blocks)))
  ([block i blocks]
    (lazy-seq
      (if (= i BLOCK_SIZE)
        (stream-bytes blocks)
        (cons (nth block i) (stream-bytes block (inc i) blocks))))))

(defn- init-ctrblk
  "Create a counter block with CTR set to 1 (lsb)."
  [nonce iv]
  (byte-array
    (for [i (range BLOCK_SIZE)]
      (let [j (- i NONCE_SIZE)]
        (cond
          (< i NONCE_SIZE) (nth nonce i)
          (< j IV_SIZE) (nth iv j)
          (not= i (dec BLOCK_SIZE)) (byte 0)
          :else (byte 1))))))

(defn stream-aes-ctr
  "Generate a stream of bytes from the initial data, using AES in counter mode.

  This is tested against the three 128-bit test vectors in
  [RFC3686](http://www.faqs.org/rfcs/rfc3686.html) - the bytes returned
  match those expected for the key stream."
  [key nonce iv]
  (let [cipher (Cipher/getInstance CIPHER_SPEC)
        key (SecretKeySpec. key CIPHER)
        ctrblk (init-ctrblk nonce iv)]
    (do
      (.init cipher Cipher/ENCRYPT_MODE key (IvParameterSpec. ctrblk))
      (stream-bytes (stream-blocks cipher)))))

;; ## Extension to widen key space

(defn- extract-bytes
  "Extract a sub-array of bytes from the given data."
  [data start length]
  (byte-array
    (for [i (range length)]
      (nth data (+ i start)))))

(defn- create-nonce-iv
  "The nonce and IV for each stream must be repeatable, but should also be
  distinct (so that we still give 'random' results with very poor keys like
  an initial data block of all zeroes).  A conservative solution, I hope,
  is to take these from the hash of the stream's index."
  [index]
  (let [hash (. MessageDigest getInstance HASH)
        data (.digest hash (byte-array [(byte index)]))]
    [(extract-bytes data 0 NONCE_SIZE)
     (extract-bytes data NONCE_SIZE IV_SIZE)]))

(defn- create-key
  "Extract a key from the data, padding with zeroes as needed."
  [data index]
  (byte-array
    (for [i (range KEY_SIZE)]
      (nth data (+ i (* index KEY_SIZE)) 0))))

(defn- single
  "Create a stream of random bytes using AES in CTR mode, with a key from the
  given data, and a nonce and IV from the hash of the stream index."
  [data index]
  (let [key (create-key data index)
        [nonce iv] (create-nonce-iv index)]
    (stream-aes-ctr key nonce iv)))

(defn- parallel
  "Combine multiple byte-streams in parallel, using XOR to merge the results."
  [streams]
  (lazy-seq
    (let [b (map first streams)
          streams (map rest streams)]
      (cons (apply bit-xor b) (parallel streams)))))

(defn random
  "Create a stream of random bytes, merging sufficient parallel streams
  to consume all the input data as an initial seed."
  [data]
  (let [len (count data)]
    (assert (> len 0) "No data to seed random stream")
    (let [n (int (/ (+ (dec KEY_SIZE) len) KEY_SIZE))]
      (if (= 1 n)
        (single data 0)
        (parallel (for [i (range n)] (single data i)))))))

;; ## Extract values from the random stream

(defn rand-signed-byte
  "Generate a pseudo-random, uniformly distributed, signed byte in the
  range [-128 127]."
  [state]
  (let [[r & state] state]
    [r state]))

(defn rand-unsigned-byte
  "Generate a pseudo-random, uniformly distributed, unsigned 'byte' in the
  range [0 255]."
  [state]
  (let [[r state] (rand-signed-byte state)]
    [(unsign-byte r) state]))

(defn rand-real
  "Generate a pseudo-random, uniformly distributed real in the range [0 1).
  Because this is calculated from a single byte only 256 distinct values are
  possible."
  [n state]
  (let [[r state] (rand-unsigned-byte state)]
    [(* n (/ r 256.0)) state]))

(defn- bitmask
  "A mask that covers the significant bits of the input, `n`."
  ([n] (if (< n 2) n (bitmask (bit-shift-right n 1) 1)))
  ([n m] (if (= 0 n) m (recur (bit-shift-right n 1) (inc (* 2 m))))))

(defn rand-byte
  "Generate a pseudo-random, uniformly distributed, unsigned 'byte' in the
  range [0 n).  This signature matches the system `rand-int` routine
  (returning values in a half-open range).

  The implementation masks and discards, rather than using modular division,
  to avoid bias."
  [n state]
  (assert (> n 0))
  (assert (< n 257))
  (let [[r state] (rand-unsigned-byte state)
        m (bitmask (dec n))
        r (bit-and r m)]
    (if (< r n) [r state] (recur n state))))

(defn rand-sign
  "Generate a pseudo-random, unbiased choice between 1 and -1."
  [state]
  (let [[r state] (rand-byte 2 state)]
    [(if (= 1 r) r -1) state]))

