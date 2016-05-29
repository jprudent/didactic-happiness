#include <Keypad.h>
#include <LiquidCrystal.h>
#include "HidKeyboard.h"
#include "PasswordGenerator.h"
#include "MultitapKeypad.h"
#include "Display.h"


/**
 * Password generator stuff
 */
const char* PASSWORD = "passw";
const char* SITE = "gmail.com";

Display *display;
MultitapKeypad *mKeypad;
void logRotate(char c) {
  Serial.print("rotate");
  Serial.println(c);
  display->replace(c);
}

void logConfirm(char c) {
  Serial.print("confirm");
  Serial.println(c);
  display->append(c);
}


/**
 * Setup & loop
 */
void setup() {
    Serial.begin(9600);
    mKeypad = new MultitapKeypad(logRotate, logConfirm);
    display = new Display();
    

  // Print a message to the LCD.);
}

void loop() {
    //loopKeypad();
    //PasswordGen"erator pwdgen;
    //char output[HLEN + 1];
    //pwdgen.generate_password(output, PASSWORD, SITE);
    //Serial.println(output);
    //type_on_keyboard(output);
    //delay(2000);
    //HidKeyboard kbd;
    //kbd.type_on_keyboard("bourgogne ");
    //getLcd()->print("Made in");
    mKeypad->getKeys(); 
}
