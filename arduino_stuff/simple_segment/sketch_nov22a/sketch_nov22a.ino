
void setup() {
   pinMode(13, OUTPUT);
}

int i = 0;

void loop() {  
  digitalWrite(13, i);
  i = (i +1) % 2;
  delay(1000);
}
