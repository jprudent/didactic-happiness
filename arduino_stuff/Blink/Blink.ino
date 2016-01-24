
const int pinLed = 13;
const int pinButton = 2;

// the setup function runs once when you press reset or power the board
void setup() {
  // initialize digital pin 13 as an output.
  pinMode(pinLed, OUTPUT);
  pinMode(pinButton, INPUT);  
}

int buttonValue = LOW;

// the loop function runs over and over again forever
void loop() {
  buttonValue = digitalRead(pinButton);
  if(buttonValue == HIGH) {
    digitalWrite(pinLed, HIGH);
  } else {
    digitalWrite(pinLed, LOW);
  }
  
}
