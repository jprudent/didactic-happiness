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
  Serial.println(dichotomicKeyboard.currentLetter());
}
