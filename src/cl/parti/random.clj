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

Byte and block streams are lazy sequences; the bit stream is a function.

*IMPORTANT* - The approach and code here need review by someone more
knowledgeable than me.

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


;; ## Bit stream

(def mask-table
  "A table of binary masks to select `required` bits from the most significant
  (leftmost) bits of a byte whose least significant `available` bits are
  valid."
  (object-array
    (for [available (range 9)]
      (int-array
        (for [required (range (inc available))]
          (let [mask (dec (bit-shift-left 1 required))
                remaining (- available required)]
            (bit-shift-left mask remaining)))))))

(defn get-mask
  "Simplify access to `mask-table`."
  [required available]
  (nth (nth mask-table available) required))

(defn- transfer-bits
  "Append the leftmost `required` bits from `bits` to `value`."
  [required value bits available]
  (let [remaining (- available required)
        value (bit-shift-left value required)
        mask (get-mask required available)
        extra (bit-shift-right (bit-and bits mask) remaining)]
    (+ value extra)))

(defn stream-bits
  "The byte stream is converted to bits in the order expected when displayed
  from left to right.  So the blocks are converted in the order they are
  generated; for each block the bytes are taken in array index order; for
  each byte the buts are taken from most to least significant bit."
  ([bytes] (stream-bits (first bytes) 8 (rest bytes)))
  ([bits available bytes]
    (fn [required]
      (loop [req required
             value 0
             bits bits
             avail available
             bytes bytes]
        (cond
          (zero? req) [value (stream-bits bits avail bytes)]
          (zero? avail) (recur req value (first bytes) 8 (rest bytes))
          (<= req avail) (recur 0 (transfer-bits req value bits avail)
                           bits (- avail req) bytes)
          :else (recur (- req avail) (transfer-bits avail value bits avail)
                  (first bytes) 8 (rest bytes)))))))

;; #### Extract values
;;
;; All the extraction functions return a value and a new stream.  The
;; new stream must be used in future calls (using the old stream will
;; not trigger an error, but will return the same value as the previous
;; call).

(def ^:private ^{:doc "A lookup table for the number of bits required to
represent an unsigned byte.  Index 0 is provided to simplify indexing, but
has a meaningless value."}
  bit-table
  (int-array
    (flatten
      (cons 0
        (for [i (range 8)]
          (repeat (bit-shift-left 1 i) (inc i)))))))

(defn
  ^{:doc "Return the number of bits necessary to represent `n`."
    :test #(do
             (assert (= 0 (n-bits 0)))
             (assert (= 1 (n-bits 1)))
             (assert (= 2 (n-bits 2)))
             (assert (= 2 (n-bits 3)))
             (assert (= 8 (n-bits 255)))
             (assert (= 9 (n-bits 256)))
             (assert (= 9 (n-bits 257))))}
  n-bits
  ([n] (n-bits n 0))
  ([n acc]
    (cond
      (zero? n) acc
      (< n 256) (+ acc (nth bit-table n))
      :else (recur (/ n 256) (+ acc 8)))))

(defn rand-bits
  "Similar to `rand-int`, returns a pseudo-random value from the semi-open
  [0 maximum).

  The minimum number of bits necessary to represent the largest return
  value are taken from the bit stream.  If this is less than `maximum`
  then it is returned, otherwise it is discarded and the process repeated.
  This gives an unbiased, uniformly distributed value, assuming that the
  bit stream itself is unbiased."
  [maximum state]
  (let [size (n-bits (dec maximum))]
    (loop [state state]
      (let [[r state] (state size)]
        (if (< r maximum) [r state] (recur state))))))

(defn rand-bits-symmetric
  "Return a pseudo-random value from [-maximum maxiumum]."
  [maximum state]
  (let [[r state] (rand-bits (inc (* 2 maximum)) state)]
    [(- r maximum) state]))

(defn rand-sign
  "Generate a pseudo-random, unbiased choice between 1 and -1."
  [state]
  (let [[r state] (rand-bits 1 state)]
    [(if (= 1 r) r -1) state]))

(defn rand-real
  "Generate a pseudo-random, uniformly distributed real in the range [0 1).
  Because this is calculated from a single byte only 256 distinct values are
  possible."
  [n state]
  (let [[r state] (rand-bits 8 state)]
    [(* n (/ r 256.0)) state]))


;; ## Byte stream

(defn- stream-unsigned-bytes
  "Convert a stream of blocks to a stream of unsigned bytes.

  The first form re-calls with the head block and a zero offset.

  The second form recurses through the available bytes in the block and
  then re-calls with the remaining blocks."
  ([blocks] (stream-unsigned-bytes (first blocks) 0 (rest blocks)))
  ([block i blocks]
    (lazy-seq
      (if (= i BLOCK_SIZE)
        (stream-unsigned-bytes blocks)
        (cons
          (unsign-byte (nth block i))
          (stream-unsigned-bytes block (inc i) blocks))))))

;; ## Block stream (Counter mode cipher)
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
  "Generate a stream of unsigned bytes from the initial data, using AES in
  counter mode.

  This is tested against the three 128-bit test vectors in
  [RFC3686](http://www.faqs.org/rfcs/rfc3686.html) - the bytes returned
  match those expected for the key stream."
  [key nonce iv]
  (let [cipher (Cipher/getInstance CIPHER_SPEC)
        key (SecretKeySpec. key CIPHER)
        ctrblk (init-ctrblk nonce iv)]
    (do
      (.init cipher Cipher/ENCRYPT_MODE key (IvParameterSpec. ctrblk))
      (stream-unsigned-bytes (stream-blocks cipher)))))

;; #### Extension to widen key space

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
      (nth data (+ i (* index KEY_SIZE)) (byte 0)))))

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

;; #### Main interface

(defn random-bits
  "Create a stream of random bits from the given data."
  [data]
  (let [len (count data)]
    (assert (> len 0) "No data to seed random stream")
    (let [n (int (/ (+ (dec KEY_SIZE) len) KEY_SIZE))]
      (stream-bits
        (if (= 1 n)
          (single data 0)
          (parallel (for [i (range n)] (single data i))))))))
