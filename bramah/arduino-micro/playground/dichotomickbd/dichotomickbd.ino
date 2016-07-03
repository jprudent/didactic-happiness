#include <Arduino.h>

#include "dichotomicKeyboard.h"
#include "Display.h"
#include "PasswordGenerator.h"
#include <LiquidCrystal.h>

#define STEP_INPUT 0
#define STEP_GENERATE 1

#define BTN_LEFT 2
#define BTN_RIGHT 3
#define BTN_SELECT 4
#define BTN_ERASE 5
#define BTN_OK 6

Keys * alpha = new Keys("abcdefghijklmnopqrstuvwxyz", 26);
Keys ** keys = new Keys*[1] {alpha};
DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);
PasswordGenerator passwordGenerator = PasswordGenerator();
Display display = Display();
int PHASE = STEP_INPUT;

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
  pullupMode(BTN_OK);
}

void generate() {
  char generatedPassword[HLEN];

  char * line1 = display.getLine1();
  char website[strlen(line1)];
  strcpy(website, line1);
  website[strlen(website) - 1] = '\0';

  Serial.println(website);
  passwordGenerator.generate_password(generatedPassword, "GOD_PASSWORD", website);
  Serial.println(generatedPassword);
}

void loop() {
  if (PHASE == STEP_INPUT) {
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
    } else if (isBtnPressed(BTN_OK)) {
        PHASE = STEP_GENERATE;
        generate();
    }

    if(btnPressed) {
      delay(400);
    }

  }



  delay(100);
}

boolean isBtnPressed(int btnPin) {
  return ! isPinHigh(btnPin);
}

boolean isPinHigh(int pin){
  return digitalRead(pin) == HIGH;
}
