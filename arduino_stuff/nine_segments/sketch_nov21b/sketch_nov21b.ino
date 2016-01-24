// A = 7, B= 6, C= 4, D= 2, E= 1, F= 9, G=10
#define SEGMENT_A 8
#define SEGMENT_B 9
#define SEGMENT_C 6
#define SEGMENT_D 5
#define SEGMENT_E 3
#define SEGMENT_F 10
#define SEGMENT_G 11

#define BTN_PIN 4

const byte font0[] = {
    1,
  1,  1,
    0,
  1,  1,
    1,
};


const byte font1[] = {
    0,
  0,  1,
    0,
  0,  1,
    0,
};


const byte font2[] = {
    1,
  0,  1,
    1,
  1,  0,
    1,
};


const byte font3[] = {
    1,
  0,  1,
    1,
  0,  1,
    1,
};


const byte font4[] = {
    0,
  1,  1,
    1,
  0,  1,
    0,
};


const byte font5[] = {
    1,
  1,  0,
    1,
  0,  1,
    1,
};


const byte font6[] = {
    1,
  1,  0,
    1,
  1,  1,
    1,
};


const byte font7[] = {
    1,
  0,  1,
    0,
  0,  1,
    0,
};


const byte font8[] = {
    1,
  1,  1,
    1,
  1,  1,
    1,
};


const byte font9[] = {
    1,
  1,  1,
    1,
  0,  1,
    1,
};

const byte segments[] = {
  SEGMENT_A,
  SEGMENT_F,
  SEGMENT_B,
  SEGMENT_G,
  SEGMENT_E,
  SEGMENT_C,
  SEGMENT_D
};

const byte* digits[10] = {
  font0, font1, font2, font3, font4, font5, font6, font7, font8, font9
};

void printDigit(byte digit) {
  const byte* digitArray = digits[digit];
  for(int i=0; i<7; i++) {
    digitalWrite(segments[i], digitArray[i]);
  }
}


void setup() {
  pinMode(BTN_PIN, INPUT);
  for(byte i=0; i < sizeof(segments); i++){
    pinMode(segments[i], OUTPUT);
    digitalWrite(segments[i], 0);
  }
}

byte pushBtnCnt = 0;

void loop() {  
  if(digitalRead(BTN_PIN) == HIGH) {
    pushBtnCnt = (pushBtnCnt + 1) % 10;  
    delay(200);
  }
  printDigit(pushBtnCnt);
}
