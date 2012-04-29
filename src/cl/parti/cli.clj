(ns cl.parti.cli)

; provide access to mosaic generation for various different tasks (this
; also demonstrates the functionality of the various modules):
; - as a web server, running on a particular port, which returns mosaic
;   images for the configured configuration (size, border, etc).
; - as a command line utility that generates a mosaic for a given input
;   value and configuration.
; - as a command line utility that checksums a file and gives a
;   standard image.

