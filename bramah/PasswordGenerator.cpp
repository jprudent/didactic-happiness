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

// Compute the hash of s1 + s2
// output: the output string of size HLEN
void PasswordGenerator::hash(char * output, const char * s1, const char * s2) {
    hashcode.reset();
    hashcode.update(s1, strlen(s1));
    hashcode.update(s2, strlen(s2));
    hashcode.finalize(output, HLEN);
};


// Encode input in ASCII readable output
// output must have HLEN size
// output may not be reversable to input
void PasswordGenerator::encode_to_ascii(char * output, char * input) {
    const char range = '~' - '!';
    for(int i=0; i<HLEN; i++) {
        output[i] = ((unsigned char)input[i] % range) + 33;
    }
};

// output: the output string of size HLEN
void PasswordGenerator::generate_password(char * output, const char * master_password, const char * memo) {
    char sha[HLEN];
    hash(sha, master_password, memo);
    encode_to_ascii(output, sha);
};

