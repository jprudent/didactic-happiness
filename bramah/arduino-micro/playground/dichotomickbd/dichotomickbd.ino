#include "dichotomicKeyboard.h"

Keys * alpha = new Keys("abcdefghijklmnopqrstuvwxyz", 26);
Keys ** keys = new Keys*[1] {alpha};
DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);

void setup() {
  Serial.begin(9600);
}

void loop() {
  delay(3000);
  Serial.println("hello");
/*  Serial.println("currentLetter");
  Serial.println(dichotomicKeyboard.currentLetter());
  Serial.println(alpha->symbols);
  Serial.println(keys[0]->symbols);
  Serial.println(dichotomicKeyboard.keys[0]->symbols); */
  Serial.println(dichotomicKeyboard.keys[0]->symbols);
  Serial.println(dichotomicKeyboard.currentLetter());
  dichotomicKeyboard.left();
  Serial.println(dichotomicKeyboard.currentLetter());
}
