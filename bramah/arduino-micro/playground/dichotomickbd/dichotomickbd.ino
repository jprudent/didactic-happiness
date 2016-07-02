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

void pullupMode(byte pin) {
  pinMode(pin, INPUT);
  digitalWrite(pin, HIGH);
}

void setup() {
  Serial.begin(9600);
  pullupMode(BTN_RIGHT);
  pullupMode(BTN_LEFT);
  pullupMode(BTN_SELECT);
}

void loop() {
  bool btnPressed = false;
  char symbol = '\0';
  if(display.getLine1()[0] == '\0') {
    symbol = dichotomicKeyboard.currentLetter();
    display.replace(symbol);
  } else if(isBtnPressed(BTN_LEFT)) {
    symbol = dichotomicKeyboard.left();
    display.replace(symbol);
    Serial.println(symbol);
    btnPressed = true;
  } else if (isBtnPressed(BTN_RIGHT)) {
    symbol = dichotomicKeyboard.right();
    display.replace(symbol);
    Serial.println(symbol);
    btnPressed = true;
  } else if(isBtnPressed(BTN_SELECT)) {
    Serial.println("SELECT");
    symbol = dichotomicKeyboard.currentLetter();
    display.append(symbol);
    Serial.println(symbol);
    dichotomicKeyboard.reset();
    display.replace(dichotomicKeyboard.currentLetter());
    btnPressed = true;
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
