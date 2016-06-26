#ifndef MULTITAP_KEYPAD_H
#define MULTITAP_KEYPAD_H

#include <stdio.h>
#include <Keypad.h>

class MultitapKeypad {
    public:
      MultitapKeypad(
        void (*onRotateKeyCb)(char),
        void (*onConfirmKeyCb)(char));
      ~MultitapKeypad();
      void readKeypad();
    private:
      Keypad *numberKeypad;
      Keypad *letterKeypad;
      Keypad *currentKeypad;
      void (*onRotateKeyCb)(char);
      void (*onConfirmKeyCb)(char);
      void keypadEvent_ltr(char key); 
      void keypadEvent_num(char key);
      void swOnState(char key, KeyState kpadState);
      void onKeyChange(Keypad keypad);
};

#endif
