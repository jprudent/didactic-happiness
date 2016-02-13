#!/usr/bin/python

name = "Root-Me"
edi = 2
ebp20 = 0

for c in name:
    ebp28 = 1
    for cpt in range(0,edi):
        ebp28 = ebp28 * ord(c)
    ebp20 = ebp20 + ebp28
    edi = edi +1

print(ebp20)
