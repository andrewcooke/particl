(ns cl.parti.random-test
  (:use clojure.test)
  (:use (cl.parti random utils))
  (:use clojure.math.numeric-tower))


; compare against the test vectors in rfc36868

(defn assert-vector [key nonce iv target]
  (let [result
        (byte-array
          (map sign-byte
            (take (count target) (stream-aes-ctr key nonce iv))))
        result (format-hex result)
        target (format-hex target)]
    (printf "target %s\n" target)
    (printf "result %s\n" result)
    (is (= target result))))

(deftest test-vector-1
  (assert-vector
    (parse-hex "AE6852F8121067CC4BF7A5765577F39E")
    (parse-hex "00000030")
    (parse-hex "0000000000000000")
    (parse-hex "B7603328DBC2931B410E16C8067E62DF")))

(deftest test-vector-2
  (assert-vector
    (parse-hex "7E24067817FAE0D743D6CE1F32539163")
    (parse-hex "006CB6DB")
    (parse-hex "C0543B59DA48D90B")
    (parse-hex "5105A305128F74DE71044BE582D7DD87FB3F0CEF52CF41DFE4FF2AC48D5CA037")))

(deftest test-vector-3
  (assert-vector
    (parse-hex "7691BE035E5020A8AC6E618529F9A0DC")
    (parse-hex "00E0017B")
    (parse-hex "27777F3F4A1786F0")
    (parse-hex "C1CE4AAB9B2AFBDEC74F58E2E3D67CD85551B638CA786E21CD8346F1B2EE0E4C0593250C17553600A63DFECF562387E9")))

; various details of bit-streams

(deftest test-mask
  (is (= 1 (get-mask 1 1)))
  (is (= 255 (get-mask 8 8)))
  (is (= 2 (get-mask 1 2)))
  (is (= 254 (get-mask 7 8))))

(deftest test-n-bits
  (test #'n-bits))

(deftest test-stream-bits
  (let [s0 (stream-bits [2r11110000 2r10101010])
        [_ s4] (s0 4)
        [_ s8] (s0 8)]
    (is (= 0 (first (s0 0))))
    (is (= 2r1 (first (s0 1))))
    (is (= 2r111100 (first (s0 6))))
    (is (= 2r1111000010 (first (s0 10))))
    (is (= 2r1111000010101010 (first (s0 16))))
    (is (= 2r00001 (first (s4 5))))
    (is (= 2r101 (first (s8 3))))))

; expect a given byte to be repeated approx every 256 times,
; with an sd of 16.

(defn lengths
  "Measure distance between repated values."
  ([r] (let [[a r] (rand-bits 256 r)] (lengths r a 1)))
  ([r a n]
    (let [[b r] (rand-bits 256 r)]
      (if (= a b)
        (lazy-seq (cons n (lengths r a 1)))
        (recur r a (inc n))))))

(deftest test-lengths
  "Check the lengths function itself."
  (is (= [1 3 2] (take 3 (lengths (stream-bits [0 0 1 1 0 1 0 0]))))))

(defn sqr [x] (* x x))

(defn assert-stats [s mean sd err]
  (let [n (float (count s))
        s-mean (/ (apply + s) n)
        s-sd (sqrt (/ (apply + (map #(sqr (- % s-mean)) s)) n))]
    (println "mean" (double s-mean) "sd" (double s-sd))
    (is (< s-mean (* (+ 1 err) mean)))
    (is (> s-mean (* (- 1 err) mean)))
    (is (< s-sd (* (+ 1 err) sd)))
    (is (> s-sd (* (- 1 err) sd)))))

(deftest measure-repeat
  "Check stats of lengths of sequences between same value."
  (let [r (random-bits (byte-array 1 (byte 1)))
        l (lengths r)]
    (assert-stats (take 1000 l) 256 256 0.1))
  (let [r (random-bits (byte-array 64 (byte 1)))
        l (lengths r)]
    (assert-stats (take 1000 l) 256 256 0.1)))

(defn filter-dups [s values]
  (if-let [values (seq values)]
    (let [[n i v] (first values)]
      (if (s v)
        (do
          (println "duplicate!" n i v)
          (recur s (rest values)))
        (recur (conj s v) (rest values))))
    s))

(deftest check-bit-sensitive
  "Every different bit stream should return a different 16 bit integer
  (on average!).  The input data are [1 1 ... 1] for different sizes,
  and with each 1 in turn swapped to 2, to give some kind of sweep of
  possible inputs."
  (let [sizes [1 127 128 129 255 256 257 512]
        values
        (for [n-bits sizes
              index (range n-bits)]
          (let [hash
                (byte-array
                  (for [i (range n-bits)]
                    (byte (if (= i index) 1 2))))]
            [n-bits index (rand-bits 16 (random-bits hash))]))
        s (filter-dups #{} values)]
    ;    (println values)
    ;    (println s)
    (is (= (apply + sizes) (count s)))))

(defn rand-stream [f state]
  (lazy-seq
    (let [[r state] (f state)]
      (cons r (rand-stream f state)))))

(deftest test-rubho
  "Test mean and SD against that expected for uniform distribution.  Only
  use values > 11 as stats influenced by discrete values."
  (doseq [n (range 12 256)]
    (let [s (random-bits (byte-array 1 (byte (bit-and n 127))))
          r (rand-stream (partial rand-bits n) s)]
      (assert-stats (take 10000 r) (/ (dec n) 2.0) (/ (dec n) (sqrt 12)) 0.1))))

(run-tests)
