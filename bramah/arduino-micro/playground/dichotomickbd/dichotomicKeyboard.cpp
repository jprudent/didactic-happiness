#include "dichotomicKeyboard.h"
#include "math.h"

void checkArgument(bool condition) {
  if(!condition) {
     // should throw an exception here
  }
}

Keys::Keys(const char * symbols, size_t len) {
  checkArgument(len > 0);
  this->symbols = symbols;
  this->len = len;
}

char Keys::letterAt(size_t index) {
  return symbols[index];
}

size_t Keys::maxIndex() {
  return len - 1;
}


Slice::Slice(size_t a, size_t b) {
  checkArgument(b >= a);
  this->a = a;
  this->b = b;
}

Slice::Slice(size_t b) : Slice(0, b) {
}

void Slice::left() {
  int size = this->b - this->a + 1;
  this->b = fmax(this->a, this->b - ceil(size / 2.0));
}

void Slice::right() {
  int size = this->b - this->a + 1;
  this->a = fmin(this->b, this->a + ceil(size / 2.0));
}

size_t Slice::half() {
  return this->a + halfSize();
}

size_t Slice::size() {
  return this->b - this->a;
};

size_t Slice::halfSize() {
  return (int)(size() / 2);
}

Keys ** deepcopy(Keys ** copy, size_t nbKeys) {
  Keys** ret = new Keys*[nbKeys];
  for(size_t i=0; i<nbKeys; i++) {
    ret[i] = new Keys(*copy[i]);
  }
  return ret;
}

DichotomicKeyboard::DichotomicKeyboard(Keys** keyboards, size_t nbKeys) {
  checkArgument(nbKeys > 0);
  this->keys = deepcopy(keyboards, nbKeys);
  this->currentKeysIndex = 0;
  this->nbKeys = nbKeys;
  this->slice = new Slice(this->currentKeys()->maxIndex());
}

DichotomicKeyboard::~DichotomicKeyboard() {
  delete keys;
  delete slice;
}

Keys* DichotomicKeyboard::switchKeyboard() {
  this->currentKeysIndex = (this->currentKeysIndex + 1) % this->nbKeys;
  return this->keys[this->currentKeysIndex];
}

char DichotomicKeyboard::left() {
  this->slice->left();
  return currentLetter();
}

char DichotomicKeyboard::right() {
  this->slice->right();
  return currentLetter();
}

char DichotomicKeyboard::currentLetter() {
  return currentKeys()->letterAt(this->slice->half());
}

void DichotomicKeyboard::reset() {
  resetSlice();
}

Keys * DichotomicKeyboard::currentKeys() {
  return keys[currentKeysIndex];
}

void DichotomicKeyboard::resetSlice() {
  this->slice = new Slice(this->currentKeys()->maxIndex());
}
