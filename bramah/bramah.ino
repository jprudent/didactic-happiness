#include <Keypad.h>
#include <ctype.h>
#include "HidKeyboard.h"
#include "PasswordGenerator.h"
#include "MultitapKeypad.h"

/**
 * Password generator stuff
 */
const char* PASSWORD = "passw";
const char* SITE = "gmail.com";

MultitapKeypad *mKeypad;
void logRotate(char c) {
  Serial.print("rotate");
  Serial.println(c);
}

void logConfirm(char c) {
  Serial.print("confirm");
  Serial.println(c);
}

/**
 * Setup & loop
 */
void setup() {
    Serial.begin(9600);
    mKeypad = new MultitapKeypad(logRotate, logConfirm);
    mKeypad->setup();
}

void loop() {
    //loopKeypad();
    //PasswordGenerator pwdgen;
    //char output[HLEN + 1];
    //pwdgen.generate_password(output, PASSWORD, SITE);
    //Serial.println(output);
    //type_on_keyboard(output);
    //delay(2000);
    //HidKeyboard kbd;
    //kbd.type_on_keyboard("bourgogne ");
    mKeypad->getKeys(); 
}
