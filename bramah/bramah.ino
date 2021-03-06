#include <Keypad.h>
#include <LiquidCrystal.h>
#include "HidKeyboard.h"
#include "PasswordGenerator.h"
#include "MultitapKeypad.h"
#include "Display.h"


const char* GOD_PASSWORD = "passw";

Display *display;
MultitapKeypad *multitapKeypad;
PasswordGenerator *passwordGenerator;
HidKeyboard *hidKeyboard;

void logRotate(char c) {
  if (c != '\n' && c != '\b') {
    display->replace(c);
  }
}

bool GENERATED = false;
char * PASSWORD;

void logConfirm(char c) {
  if (GENERATED) {
    if (c == '\n') {
      hidKeyboard->type_on_keyboard(PASSWORD);
    }
  } else {
    if (c == '\n') {
      char* site = display->getLine1();
      passwordGenerator->generate_password(PASSWORD, GOD_PASSWORD, site);
      display->reset();
      display->append(PASSWORD);
      GENERATED = true;
    } else if (c == '\b') {
      display->erase();
    } else {
      display->append(c);
    }
  }
}


/**
 * Setup & loop
 */
void setup() {
  PASSWORD = (char *)malloc((HLEN + 1) * sizeof(char));
  Serial.begin(9600);
  multitapKeypad = new MultitapKeypad(logRotate, logConfirm);
  display = new Display();
  passwordGenerator = new PasswordGenerator();
  hidKeyboard = new HidKeyboard();

  // Print a message to the LCD.);
}

void loop() {
  multitapKeypad->readKeypad();
}
