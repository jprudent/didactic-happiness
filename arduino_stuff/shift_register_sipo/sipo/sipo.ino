#define SER 4
#define SRCLK 5
#define RCLK 6


void setup() {
  pinMode(SER, OUTPUT);
  pinMode(SRCLK, OUTPUT);
  pinMode(RCLK, OUTPUT);
}

int bitmap[8] = {0,0,0,1,1,0,0,0};
int stepCnt = 0;
int ms = 500;

void loop() {

  digitalWrite(RCLK, LOW);

  prepareBitmap();
  writeBitmap();
  incStepCnt();
  incDelay();
  
  digitalWrite(RCLK, HIGH);
  delay(ms);
}

void incDelay(){
  ms = 50 + stepCnt * 20;
}

void incStepCnt() {
  stepCnt = (stepCnt + 1) % 4;
}
void prepareBitmap() {
  for(int i = 0; i < 4; i++) {
    int v = stepCnt == i;
    bitmap[i] = v;
    bitmap[7 - i] = v;
  }
}

void writeBitmap(){
  for(int i = 0; i < 8; i++) {
    shift(bitmap[i]);
  }
}


void shift(int v) {
  digitalWrite(SRCLK, LOW);
  digitalWrite(SER, v);
  digitalWrite(SRCLK, HIGH);
}

