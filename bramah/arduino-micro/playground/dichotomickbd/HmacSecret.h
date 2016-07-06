#ifndef HMAC_SECRET_H
#define HMAC_SECRET_H

#include <stdlib.h>
#include "RandomSource.h"

typedef struct HmacSecretData {
  long magicNumber;
  char hmacSecret[16];
  char version;
} HmacSecretData;

class HmacSecret {
  public:
    HmacSecret(RandomSource randomSource);
    ~HmacSecret();
    char * secretHmac();
    void setup();
  private:
    void readSecret();
    void writeSecret();
    bool isInitialized();
    void fillHmacSecretData();
    HmacSecretData hmacSecretData;
    RandomSource randomSource;
};

#endif
