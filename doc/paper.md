% Towards Graphical Hashes
% Andrew Cooke (andrew@acooke.org)
% June 16 2012

# Introduction 

What is a graphical hash?  What properties apply to them (parallel to crypto
hashes)?

If we assume that all possible images are generated uniformly then the
effective hash size of a graphical image is the number of practically distinct
images.


- Contributions

    - Architecture

    - Possible algorithm

    - Experience on what does not work

# Previous Work

- SSH

- Gravitar

# Loosely--Coupled Architecture

Decoupling image generation from the details of the hash allows each to be
changed independently.  Here the original hash is used to seed a secure PRNG;
the resulting stream of random bits is then used to construct the image.

## Cryptographic PRNG

An input hash of length $l$ is zero--padded and used to key $\lceil l/128
\rceil$ AES ciphers (128 bit key) in counter mode, following
[RFC3686](http://www.faqs.org/rfcs/rfc3686.html).  The nonce and IV for the
$n$th cipher are taken from the SHA-1 hash of $n-1$.  Output from the ciphers
is merged using bitwise exclusive--or.

This construction is intended to provide a secure, unlimited, repeatable
sequence of pseudo--random bits that has an avalanche for all bits in the
original hash, whatever the size, and has internal state at least equal to the
input.  It should also be robust to poorly chosen input data (should produce
"random" results even when input data are not hashed).

Any block cipher could be used; AES with a 128 bit key was chosen because it
is readily available.  In particular, it appears in the [Java Cryptography
Architecture](http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html).

## Image Generation

The image is a square mosaic, containg $n \times n$ square, uniformly coloured
tiles, on a black background.

The algorithm sketched below generates diagonally symmetric mosaics, for a
wide range of sizes, with structure on all scales, and with hue and lightness
changes (anti--)correlated.  It can be separated into three steps: build;
normalize; render.

### Build

The image is represented as an $n \times n$ array of integers, initially
zeroes.  All random choices use the PRNG bit stream.

A rectangular region of the image is selected at random.  Rectangles that lie
completely within the upper--right triangle of the image are discarded;
otherwise an offset (described below) is added to all pixels in the rectangle.
This process is repeated until $n^2$ rectangular patches (typically with
multiple, overlapping regions) have been modified.

The offset is an integer selected at random from the range $[-d, d]$ where
$d-1$ is the minimum $L_1$ (Manhattan) distance between the rectangle and the
image centre.  This weighting is intended to counteract the statistical bias
towards overlapping rectangles in the centre of the image.

At the end of the build process the lower--left triangle of the image is
reflected into the upper--right, giving a diagonally--symmetric pattern.

### Normalize

The integer array is histogram--equalized, to improve contrast, and mapped to
floating point values in the range $(-1, 1)$.

For insecure use (eg. abstract avatars) sigmoid--based normalization can be
substituted.  This gives a softer, more attractive result.

### Render

The $(-1, 1)$ floating point values, $v_i$, are converted to colours in HSL
space (values in $[0, 1)$), with full saturation.  Hue is $(a + b v_i) \mod
1$, where $a$ is a random value and $b$ a constant (for all images), chosen to
give a reasonable range of colours.  Lightness is $c v_i$, clipped to $[0, 1)$
where $c$ is a constant chosen to give a reasonable range of brightnesses.
The relative signs of $b$ and $c$ are chosen at random (per image).

50% of images are rotated through $90^\circ$.  This switches the axis of
symmetry to the other diagonal.

Finally, the data are expanded to give multiple pixels per value (to fill each
tile) and padded with the background colour.

# Hash Size and Collisions

## Ballpark Estimates

Pixels are generated as offsets in hue and lightness (either correlated or
anti--correlated) relative to some base value.  If a user can distinguish
$N_H$ base hues then before considering pixel values we have $2 N_H$ possible
images.

If the offset for each pixel is independent, and a user can distinguish
between $N_p$ different values, then there are $2 N_H N_p^{n^2}$ possible
images.  Diagonal symmetry reduces this to $4 N_H N_p^{n(n+1)/2}$.

But pixels are not independent.  A more realistic model might describe
lightness as the sum of levels at various scales.  Below are two size
estimates, focussing on small and large scales, respectively.

A power law distribution of scales is numerically dominated by small regions
(pixels).  So if we consider each region to be independent then the we have
$\sim 4 N_H N_r^{n(n+1)/2}$ images, where $N_r$ is the number of possible
values per region.  But for different scales to be distinguished --- for the
larger features to stand out from the pixel--to--pixel noise --- we must
emphasize the lower frequencies.  This reduces the available dynamic range to
the smaller, more numerous regions.  So $N_r$ is restricted to, perhaps, 2
bits, rather than the 6 or 7 a user might distinguish for an isolated pixel.

If $n=16$, $N_p=4$ and $N_H=32$ then we have a "hash size" of $4\times
32\times 4^{16\times15 / 2} = 2^{247}$.

The "impression" of an image --- what can be easily remembered --- is
dominated by a small number of large scale structures, whose absolute
intensities are obscured by the small--scale pixel variations.  From this
perspective, there are effectively only $4 N_H N_l^{m(m+1)/2}$ where $N_l$ is
the number of distinct, large scale intensities and $m$ is the spatial
frequency of the structure.

If $n=16$, $m=2$ $N_l=8$ and $N_H=32$ then we have a "hash size" of $4\times
32\times 8^{3} = 2^{16}$.

## Bits Consumed

![Distribution of bits consumed by mosaic size.  The y--axis is
${\rm bits}/n^2$.](bits.pdf)

The number of bits consumed from the PRNG is roughly $5 \lceil\log_2 n\rceil
n^2$, since each of the $n^2$ iterations of the image builder requires two
coordinates and a distance, all of size $\sim n$.

Counter--intuitively, the normalized number of bits consumed increases with
smaller $n$ for any given value of $\lceil\log_2 n\rceil$.  This is because
more bits are discarded to generate unbiased variates for smaller $n$.  For
example, to generate a random value $< 5$, a 3--bit integer is read.  If that
value is $\ge 5$ it is discarded and a new value read.

In any case, the total number of bits rapidly exceeds the size of the initial,
numerical hash as $n$ increases and, for practical mosaics, does not
constraint the variety of available images.

## Searching for Collisions

Hash size can be inferred from the number of collisions in a random sample.
This is only feasible for small mosaics, where collisions can be found in a
reasonable time.

For various mosaic sizes, a set of $10^7$ mosaics was generated and saved in a
compact binary format.  Similar images were then identified using
locality--sensitive hashing; repeating the hashing and counting matches per
pair allowed the candidate matches to be ordered for visual inspection.

The mosaics were stored as byte arrays, with one byte per pixel, storing only
the diagonal and lower triangle.  Normalized pixels, $(-1, 1)$, were mapped
linearly to unsigned bytes, $[0, 255]$.

The hash was constructed from the most significant bits of values at random
locations (without replacement) in the byte arrays.  The numbers of locations
and bits were hand--tuned to give a reasonable number of matches.  Locations
were varied on each iteration.

## L_2 Difference

![ECDF by mosaic size](ecdf-1.pdf)

![ECDF detail](ecdf-3.pdf)





- Ballpark estimates

- Estimate from collisions

    * Hard.

        - Only small mosaics.

        - Locality sensitive hashing.

    * How to scale?

        - Compare fluctuations in large and small mosaics

- Extending (holes)

- What is a good value anyway?

    * Cost of finding matches higher than simple hashes

- Tried to use simple sum of differences

    - Wasn't Gaussian (Q-Q plots) - too many close outliers

    - Pair + noise didn't help

## Comparison Classes

The "size" of a graphical hash can be defined as the reciprocal of the
probability of *apparent* collision.  However, whether two images appear equal
will depend on context.  There are two broad classes:

#. Two images displayed together.

#. Comparison of a displayed image with one from memory.

These overlap considerably --- even when both images are visible, conscious
focus is sequential and relies on short--term memory.  

