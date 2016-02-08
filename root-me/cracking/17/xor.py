#!/usr/bin/python

i = 0
x = [0x08, 0x04, 0x8f, 0xc1]
x.reverse()
xi = 0
bytes_read = open("ch13", "rb").read()
of = open("xored", "wb")
for b in bytes_read:
    if (i >= 0x104 and i <= 0x2e7):
      of.write(bytearray([b^x[xi] % 2**8]))
      xi = (xi + 1) % 4
    else: 
      of.write(bytearray([b]))
    i = i+1
    
      
