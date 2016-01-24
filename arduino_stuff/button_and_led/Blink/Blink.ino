
const int pinLed = 13;
const int pinLed2 = 8;
const int pinButton = 2;

// the setup function runs once when you press reset or power the board
void setup() {
  // initialize digital pin 13 as an output.
  pinMode(pinLed, OUTPUT);
  pinMode(pinButton, INPUT);  
}

// the loop function runs over and over again forever
void loop() {
  digitalWrite(pinLed, HIGH);
  digitalWrite(pinLed2, 3000);
}
