(ns cl.parti.hex)


; https://gist.github.com/581363
; from "hiredman" - confirmed public domain by email

(defn bytes-to-hex [^bytes bytes]
  (.toString
   (let [alpha "0123456789ABCDEF"]
     (areduce bytes idx ret (StringBuilder.)
       (doto ret
         (.append (.charAt alpha (int (bit-shift-right (aget bytes idx) 4))))
         (.append (.charAt alpha (int (bit-and (aget bytes idx) 0xf)))))))))

(def byte-table
  (delay ;don't generate the byte array unless needed
   (byte-array
    (map byte
         [0x00 0x01 0x02 0x03 0x04 0x05 0x06 0x07 0x08 0x09 0x00 0x00 0x00 0x00
          0x00 0x00 0x00 0x0A 0x0B 0x0C 0x0D 0x0E 0x0F]))))

(definline to-byte [hex i byte-table]
  `(byte
    (bit-or
     (bit-shift-left
      (aget ~byte-table
            (- (int (Character/toUpperCase (.charAt ~hex ~i))) 48))
      4)
     (aget ~byte-table (- (int (.charAt ~hex (inc ~i))) 48)))))

(defn hex-to-bytes [^String hex]
  (let [byte-table @byte-table
        str-len (count hex)
        product (byte-array (/ str-len 2))]
    (loop [i (int 0) n (int 0)]
      (if (> str-len i)
        (do
          (aset product n (to-byte hex i byte-table))
          (recur (int (+ i 2)) (inc n)))
        product))))
