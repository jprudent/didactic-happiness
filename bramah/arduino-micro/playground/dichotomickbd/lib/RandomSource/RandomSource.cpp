#include "RandomSource.h"
#include <Arduino.h>

RandomSource::RandomSource() {
  unsigned long seed = 0;
  randomSeed(analogRead(A0));
  for(int i = 0; i < 128; i++) {
    seed = seed ^ random();
  }
  randomSeed(seed);
}

char RandomSource::nextChar() {
  return (char) random();
}
