#include <Arduino.h>

#include "dichotomicKeyboard.h"
#include "Display.h"
#include "PasswordGenerator.h"
#include "RandomSource.h"
#include "HmacSecret.h"
#include <LiquidCrystal.h>
#include <Keyboard.h>
#include <EEPROM.h>

#define STEP_INPUT 0
#define STEP_GENERATE 1

#define BTN_LEFT 0x2100
#define BTN_RIGHT 0x2101
#define BTN_SELECT 0x2102
#define BTN_ERASE 0x2103
#define BTN_OK 0x2104

#define PIN_BUTTONS A1


Keys * alpha = new Keys("abcdefghijklmnopqrstuvwxyz", 26);
Keys ** keys = new Keys*[1] {alpha};
DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);
PasswordGenerator passwordGenerator = PasswordGenerator();
Display display = Display();
HmacSecret * hmacSecret;

int PHASE = STEP_INPUT;

void pullupMode(byte pin) {
  pinMode(pin, INPUT);
  digitalWrite(pin, HIGH);
}

void setup() {
  //Serial.begin(9600);
  //while(!Serial) {;}
  hmacSecret = new HmacSecret(RandomSource());
  hmacSecret->setup();
}

void generate() {
  char generatedPassword[HLEN];

  char * line1 = display.getLine1();
  char website[strlen(line1)];
  strcpy(website, line1);
  website[strlen(website) - 1] = '\0';

  passwordGenerator.generate_password(generatedPassword, hmacSecret->secretHmac(), website);

  Keyboard.print(generatedPassword);

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

int read_LCD_buttons(){               // read the buttons
    int adc_key_in = analogRead(PIN_BUTTONS);       // read the value from the sensor

    // my buttons when read are centered at these valies: 0, 144, 329, 504, 741
    // we add approx 50 to those values and check to see if we are close
    // We make this the 1st option for speed reasons since it will be the most likely result

    if (adc_key_in > 1000) return -1;
    //Serial.println(adc_key_in);

    if (adc_key_in < 50)   return BTN_RIGHT;
    if (adc_key_in < 250)  return BTN_SELECT;
    if (adc_key_in < 450)  return BTN_ERASE;
    if (adc_key_in < 650)  return BTN_LEFT;
    if (adc_key_in < 850)  return BTN_OK;

    return -1;                // when all others fail, return this.
}


boolean isBtnPressed(int btn) {
  return btn == read_LCD_buttons();
}

boolean isPinHigh(int pin){
  return digitalRead(pin) == HIGH;
}
