% Towards Graphical Hashes
% Andrew Cooke (andrew@acooke.org)
% June 16 2012

# Introduction 

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
$n$th cipher (zero indexed) are taken from the SHA-1 hash of $n$.  Output from
the ciphers is merged using bitwise exclusive--or.

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

# Hash Size

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
