#include "SHA256.h"
#include "Base64.h"

const char* PASSWORD = "passw";
const char* SITE = "gmail.com";
const size_t HLEN = 32;

SHA256 hashcode;

void debug(const char * s) {
  Serial.println(s);
}

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
  memcpy(output, b64encoded, HLEN);
}

// output: the output string of size HLEN
void generate_password(char * output, const char * master_password, const char * memo) {
  char sha[HLEN];
  hash(sha, master_password, memo);
  encode_to_ascii(output, sha);
}

const size_t KBD_BUF_SIZE = 8;
const char KEY_LEFT_SHIFT = 0x02; 
const char KEY_QUESTION_MARK = 0x38;
const char * SIMPLE_KEYS = "abcdefghijklmnopqrstuvwxyz1234567890\x0A\x1B\x08\t -=[]\\\0;'`,./\0"; 
const char * SHIFT_KEYS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()\0\0\0\0\0_+{}|\0:\"~<>?";
const size_t MODIFIER_IDX = 0;
const size_t KEY1_IDX = 2;

size_t index_of(const char ascii_char, const char * s) {
  for(size_t i=0; i<strlen(s); i++) {
    if(s[i] == ascii_char) {
      return i;
    }
  }
  return -1;
}

char to_hid_page_7(uint8_t * keyboard_buffer, const char ascii_char) {
  size_t index = index_of(ascii_char, SIMPLE_KEYS);
  if(index != -1) {
    keyboard_buffer[MODIFIER_IDX] = 0;
    keyboard_buffer[KEY1_IDX] = index + 0x04; // because a = 4 
  } else {
    index = index_of(ascii_char, SHIFT_KEYS);
    keyboard_buffer[MODIFIER_IDX] = KEY_LEFT_SHIFT;
    if(index != -1) {
      keyboard_buffer[KEY1_IDX] = index + 0x04; // because a = 4
    } else {
      keyboard_buffer[KEY1_IDX] = KEY_QUESTION_MARK;
    }
  }
}

void write_keyboard(uint8_t * keyboard_buffer) {
  Serial.write(keyboard_buffer, KBD_BUF_SIZE);
}

void press_key(uint8_t * keyboard_buffer, const char ascii_char) {
  to_hid_page_7(keyboard_buffer, ascii_char);
  write_keyboard(keyboard_buffer);
}

void release_key(uint8_t * keyboard_buffer) {
  for(int i=0; i<KBD_BUF_SIZE; i++) {
    keyboard_buffer[i] = 0;
  }
  write_keyboard(keyboard_buffer);
}

void type_on_keyboard(char * input) {
  uint8_t keyboard_buffer[KBD_BUF_SIZE] = { 0 };
  for(int i=0; i<strlen(input); i++) {
    press_key(keyboard_buffer, input[i]);
    release_key(keyboard_buffer);
  }
}

void setup() {
  Serial.begin(9600);
  delay(1000);
}

void loop() {
  char output[HLEN+1];
  generate_password(output, PASSWORD, SITE);
  output[HLEN] = 0;
  type_on_keyboard(output);
  delay(2000);
}
