
bool TYPED = false;

void setup() {
  Keyboard.begin(); 
}

void loop() {
  delay(3000);
  if(!TYPED) {
    Keyboard.print("Bourgogne");
    TYPED = true;
  }
}
