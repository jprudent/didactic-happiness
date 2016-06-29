#ifndef DICHOTOMIC_KEYBOARD_H
#define DICHOTOMIC_KEYBOARD_H

#include <stdlib.h>

class Keys {
  public:
    Keys(const char * symbols, size_t len);
    char letterAt(size_t index);
    size_t maxIndex();
  //private:
    const char* symbols;
    int len;
};

class Slice {
  public:
    Slice(size_t a, size_t b);
    Slice(size_t b);
    void left();
    void right();
    size_t half();

  //private:
    size_t a;
    size_t b;
    size_t size();
    size_t halfSize();
};

class DichotomicKeyboard {
  public:

    DichotomicKeyboard(Keys ** keyboards, size_t nbKeys);
    ~DichotomicKeyboard();
    Keys* switchKeyboard();
    char left();
    char right();
    char currentLetter();
    void reset();

  //private:

    Keys ** keys;
    size_t currentKeysIndex;
    size_t nbKeys;
    Slice* slice;

    Keys * currentKeys();
    void resetSlice();

};

#endif
