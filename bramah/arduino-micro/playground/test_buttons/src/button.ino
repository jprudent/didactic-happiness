#include <Arduino.h>
#define BTN5 13
#define BTN4 A1
#define BTN3 A2
#define BTN2 A3
#define BTN1 A5


void pullupMode(byte pin) {
  pinMode(pin, INPUT);
  digitalWrite(pin, HIGH);
}

boolean isPinHigh(int pin){
  return digitalRead(pin) == HIGH;
}

boolean isBtnPressed(int btnPin) {
  return ! isPinHigh(btnPin);
}

void setup()
{
  // initialize LED digital pin as an output.
  //pinMode(LED_BUILTIN, OUTPUT);
  pullupMode(BTN5);
  pullupMode(BTN4);
  pullupMode(BTN3);
  pullupMode(BTN2);
  pullupMode(BTN1);
  Serial.begin(9600);
  delay(1000);
  Serial.println("Please press some button");
}

void loop()
{
  Serial.println("--------");
  boolean btn1 = isBtnPressed(BTN1);
  boolean btn2 = isBtnPressed(BTN2);
  boolean btn3 = isBtnPressed(BTN3);
  boolean btn4 = isBtnPressed(BTN4);
  boolean btn5 = isBtnPressed(BTN5);

  if(btn1){
    Serial.println("BTN1");
  }

  if(btn2){
    Serial.println("BTN2");
  }

  if(btn3){
    Serial.println("BTN3");
  }

  if(btn4){
    Serial.println("BTN4");
  }

  if(btn5){
    Serial.println("BTN5");
  }

  delay(1000);
}
