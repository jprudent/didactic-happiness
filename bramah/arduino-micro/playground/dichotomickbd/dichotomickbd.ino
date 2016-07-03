#include <Arduino.h>

#include "dichotomicKeyboard.h"
#include "Display.h"
#include <LiquidCrystal.h>

Keys * alpha = new Keys("abcdefghijklmnopqrstuvwxyz", 26);
Keys ** keys = new Keys*[1] {alpha};
DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);

Display display = Display();

#define BTN_LEFT 2
#define BTN_RIGHT 3
#define BTN_SELECT 4
#define BTN_ERASE 5

void pullupMode(byte pin) {
  pinMode(pin, INPUT);
  digitalWrite(pin, HIGH);
}

void setup() {
  Serial.begin(9600);
  pullupMode(BTN_RIGHT);
  pullupMode(BTN_LEFT);
  pullupMode(BTN_SELECT);
  pullupMode(BTN_ERASE);
}

void loop() {
  bool btnPressed = false;
  if(display.getLine1()[0] == '\0') {
    display.replace(dichotomicKeyboard.currentLetter());
  } else if(isBtnPressed(BTN_LEFT)) {
    display.replace(dichotomicKeyboard.left());
    btnPressed = true;
  } else if (isBtnPressed(BTN_RIGHT)) {
    display.replace(dichotomicKeyboard.right());
    btnPressed = true;
  } else if(isBtnPressed(BTN_SELECT)) {
    display.append(dichotomicKeyboard.currentLetter());
    dichotomicKeyboard.reset();
    display.replace(dichotomicKeyboard.currentLetter());
    btnPressed = true;
  } else if (isBtnPressed(BTN_ERASE)) {
    display.erase();
  }

  if(btnPressed) {
    delay(400);
  }

  delay(100);
}

boolean isBtnPressed(int btnPin) {
  return ! isPinHigh(btnPin);
}

boolean isPinHigh(int pin){
  return digitalRead(pin) == HIGH;
}
