#define BTN_1 2
#define BTN_2 3
#define BTN_3 4

void setup() {
  Serial.begin(9600);
  // activate pull up setting the pin to HIGH in INPUT mode
  pinMode(BTN_1, INPUT);
  digitalWrite(BTN_1, HIGH);
  pinMode(BTN_2, INPUT);
  digitalWrite(BTN_2, HIGH);
  pinMode(BTN_3, INPUT);
  digitalWrite(BTN_3, HIGH);
  
}

void loop() {
  printButton("B1", digitalRead(BTN_1));
  printButton("B2", digitalRead(BTN_2));
  printButton("B3", digitalRead(BTN_3));
  Serial.println("------------------------");
  delay(1100);
}

void printButton(char name[], byte v) {
  Serial.print(name);
  Serial.print(" = ");
  Serial.println(v);
}

