#include "SHA256.h"
#include "Base64.h"

const char* PASSWORD = "pass";
const char* SITE = "gmail.com";
const size_t HLEN = 32;

SHA256 hashcode;

// Compute the hash of s1 + s2
// output: the output string of size HLEN
void hash(char * output, const char * s1, const char * s2) {
  hashcode.reset();
  hashcode.update(s1, strlen(s1));
  hashcode.update(s2, strlen(s2));
  hashcode.finalize(output, HLEN);
}

// Encode input in ASCII readable output
// output must have HLEN size
// output may not be reversable to input
void encode_to_ascii(char * output, char * input) {
  const int b64l = base64_enc_len(HLEN);
  char b64encoded[b64l];
  base64_encode(b64encoded, input, HLEN);
  memcpy(output, input, HLEN);
}

// output: the output string of size HLEN
void generate_password(char * output, const char * master_password, const char * memo) {
  char sha[HLEN];
  hash(sha, master_password, memo);
  encode_to_ascii(output, sha);
}

void setup() {
  Serial.begin(9600);

  char output[HLEN];
  generate_password(output, PASSWORD, SITE);
}

void loop() {

}
