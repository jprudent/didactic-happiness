#include "HidKeyboard.h"
#include <stdio.h>
#include "string.h"


const size_t KBD_BUF_SIZE = 8;
const char KEY_LEFT_SHIFT = 0x02;
const char KEY_QUESTION_MARK = 0x38;
const char * SIMPLE_KEYS = "abcdefghijklmnopqrstuvwxyz1234567890\x0A\x1B\x08\t -=[]\\\0;'`,./\0";
const char * SHIFT_KEYS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()\0\0\0\0\0_+{}|\0:\"~<>?";
const size_t MODIFIER_IDX = 0;
const size_t KEY1_IDX = 2;

HidKeyboard::HidKeyboard() {
};

HidKeyboard::~HidKeyboard() {
};

size_t HidKeyboard::index_of(const char ascii_char, const char * s) {
  for (size_t i = 0; i < strlen(s); i++) {
    if (s[i] == ascii_char) {
      return i;
    }
  }
  return -1;
};

char HidKeyboard::to_hid_page_7(uint8_t * keyboard_buffer, const char ascii_char) {
  size_t index = index_of(ascii_char, SIMPLE_KEYS);
  if (index != -1) {
    keyboard_buffer[MODIFIER_IDX] = 0;
    keyboard_buffer[KEY1_IDX] = index + 0x04; // because a = 4
  } else {
    index = index_of(ascii_char, SHIFT_KEYS);
    keyboard_buffer[MODIFIER_IDX] = KEY_LEFT_SHIFT;
    if (index != -1) {
      keyboard_buffer[KEY1_IDX] = index + 0x04; // because a = 4
    } else {
      keyboard_buffer[KEY1_IDX] = KEY_QUESTION_MARK;
    }
  }
};

void HidKeyboard::write_keyboard(uint8_t * keyboard_buffer) {
  Serial.write(keyboard_buffer, KBD_BUF_SIZE);
};

void HidKeyboard::press_key(uint8_t * keyboard_buffer, const char ascii_char) {
  to_hid_page_7(keyboard_buffer, ascii_char);
  write_keyboard(keyboard_buffer);
};

void HidKeyboard::release_key(uint8_t * keyboard_buffer) {
  for (int i = 0; i < KBD_BUF_SIZE; i++) {
    keyboard_buffer[i] = 0;
  }
  write_keyboard(keyboard_buffer);
};

void HidKeyboard::type_on_keyboard(char * input) {
  uint8_t keyboard_buffer[KBD_BUF_SIZE] = { 0 };
  for (int i = 0; i < strlen(input); i++) {
    press_key(keyboard_buffer, input[i]);
    release_key(keyboard_buffer);
  }
};
