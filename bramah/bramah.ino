#include "SHA256.h"
#include "Base64.h"

const char* PASSWORD = "pass";
const char* SITE = "gmail.com";
const size_t HLEN = 32;

SHA256 sha256;

void setup() {
  Serial.begin(9600);
  // Essayer : http://blog.petrockblock.com/2012/05/19/usb-keyboard-with-arduino-and-v-usb-library-an-example/
  // Keyboard.begin();

  char output[HLEN];
  sha256.reset();
  sha256.update(PASSWORD, strlen(PASSWORD));
  sha256.update(SITE, strlen(SITE));
  sha256.finalize(output, HLEN);

  for (int i = 0; i < HLEN; i++) {
    Serial.write(output[i]);
  }

  int b64l = base64_enc_len(HLEN);
  char b64encoded[b64l];
  base64_encode(b64encoded, output, HLEN);
  Serial.println(b64encoded);
  //Keyboard.print(b64encoded);
}

void loop() {

}
