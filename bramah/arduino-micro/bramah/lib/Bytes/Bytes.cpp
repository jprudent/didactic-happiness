#include "Bytes.h"
#include <stdio.h>

char * Bytes::bytes_to_hexstring(unsigned char * bytes, size_t len) {
  char * out = malloc(len * 2 + 1);
  for(size_t i = 0; i < len; i++) {
    char byte = bytes[i];
    sprintf(&out[i * 2], "%02X", (unsigned char) byte);
  }
  out[len * 2] = '\0';
  return out;
}
