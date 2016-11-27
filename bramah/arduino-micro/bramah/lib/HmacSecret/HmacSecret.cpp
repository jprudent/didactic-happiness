#include "HmacSecret.h"
#include <EEPROM.h>

#define STARTING_OFFSET 21
#define VERSION 2
#define MAGIC_NUMBER 21587189

HmacSecret::HmacSecret(RandomSource randomSource) {
  this->randomSource = RandomSource(randomSource);
}

HmacSecret::~HmacSecret() {

}

void HmacSecret::readSecret() {
  this->hmacSecretData = EEPROM.get(STARTING_OFFSET, this->hmacSecretData);
}

void HmacSecret::writeSecret() {
  EEPROM.put(STARTING_OFFSET, this->hmacSecretData);
}

char * HmacSecret::secretHmac() {
  return this->hmacSecretData.hmacSecret;
}

bool HmacSecret::isInitialized() {
  return this->hmacSecretData.magicNumber == MAGIC_NUMBER;
}

void HmacSecret::setup() {
  readSecret();
  if(!isInitialized()){
    fillHmacSecretData();
    writeSecret();
  }
}

void HmacSecret::fillHmacSecretData() {
  hmacSecretData.magicNumber = MAGIC_NUMBER;
  hmacSecretData.version =  VERSION;
  for(int i = 0; i < 16; i++) {
    hmacSecretData.hmacSecret[i] = this->randomSource.nextChar();
  }
}
