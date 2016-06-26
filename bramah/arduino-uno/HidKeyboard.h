#ifndef HID_KEYBOARD_H
#define HID_KEYBOARD_H

#include <inttypes.h>
#include <stddef.h>
#include <Arduino.h>


class HidKeyboard {
    public:
        HidKeyboard();
        ~HidKeyboard();
        void type_on_keyboard(char * input);
    private:
        void release_key(uint8_t * keyboard_buffer);
        void press_key(uint8_t * keyboard_buffer, const char ascii_char);
        void write_keyboard(uint8_t * keyboard_buffer);
        char to_hid_page_7(uint8_t * keyboard_buffer, const char ascii_char);
        size_t index_of(const char ascii_char, const char * s);
};
#endif

