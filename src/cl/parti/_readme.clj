(ns ^{:doc "

## Graphical Hashes

Parti.cl is a program (and library) to generate *graphical hashes*.  In
particular, it tries to approximate *graphical cryptographic hashes*.  This
section (or the corresponding 'dummy' source file, if you reading that)
will try to explain what the phrases above mean.

A *hash* is an easy-to-compute representation of another, often much larger,
value.  Different values should, in general, give different hashes (although,
because a hash is usually much smaller than the value, it cannot *guarantee*
that every hash is different).

A *graphical hash* works in a similar way - it is a picture or image that
represents a value.  The hashes generated by this library appear as coloured
mosaics.  The colours and patterns in the mosaic change, depending on the
value.

A *cryptographic hash* is a representation with additional guarantees:

- it is hard to find another value with the same hash
- it is hard to modify the value without also changing the hash
- it is hard to find two values with the same hash

**For example,** when you download a file from a website, the site may
also display a cryptographic hash of the file.  Using the appropriate tools
you can calculate the hash of the file you downloaded and check it against
the value given on the site.  The guarantees above mean that, if the two
values match, then the downloaded file has a high probability of being
identical to the one the site owner intended to provide.

These guarantees are useful, and can be given clear definitions for hashes
that are numerical values.  But when a hash is graphical they are less
exact.

This lack of precision makes graphical hashes unattractive in many cases.
So why does this library exist?

Graphical hashes exist because numerical hashes also have problems: they
are ugly, long lines of hexadecimal digits that most people ignore.  A
graphical hash tries to engage the user - it should be attractive,
welcoming, and intuitive.

So graphical hashes are less precise, but more likely to be used.  Whether
this compromise is useful is an open question that I investigate further
elsewhere.  It is important to understand that this library is intended
only as a way of further exploring there use -
*it is experimental and open to change*.

## Code Structure

The main program is defined in `cl.parti.main` - it is a simple pipeline
that connects together functions defined in other modules.  The pieces of
the pipeline are assembled from command line arguments by the code in
`cl.parti.cli`.

The algorithm for generating the distinctive patterns is in `cl.parti.square`,
while most of the cryptographic properties depend on the routines in
`cl.parti.random`.

"
      :author "andrew@acooke.org"}
  cl.parti.-readme)

