#include "PasswordGenerator.h"
#include "SHA256.h"
#include "string.h"
#include "Arduino.h"

SHA256 hashcode;

const size_t HLEN = 32;

PasswordGenerator::PasswordGenerator() {
}

PasswordGenerator::~PasswordGenerator() {
}

// output: the output string of size HLEN
void PasswordGenerator::hash(char * output, const char * hmacSecret, const char * s) {
    hashcode.resetHMAC(hmacSecret, 16);
    hashcode.update(s, strlen(s));
    hashcode.finalizeHMAC(hmacSecret, 16, output, HLEN);
};


// Encode input in ASCII readable output
// output must have HLEN + 1 size (for 0 terminated string)
// output may not be reversable to input
void PasswordGenerator::encode_to_ascii(char * output, char * input) {
    const char range = '~' - '!';
    for(size_t i=0; i<HLEN; i++) {
        output[i] = ((unsigned char)input[i] % range) + 33;
    }
    output[HLEN] = '\0';
};

// output: the output string of size HLEN
void PasswordGenerator::generate_password(char * output, const char * hmacSecret, const char * s) {
    char sha[HLEN];
    hash(sha, hmacSecret, s);
    encode_to_ascii(output, sha);
};
