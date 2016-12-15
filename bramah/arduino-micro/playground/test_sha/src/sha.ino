#include <Arduino.h>
#include "SHA256.h"
#include "string.h"

SHA256 hashcode;

void print_array(char * a, int len) {
    for(int i = 0; i<len; i++) {
        Serial.print((unsigned byte)a[i]);
        Serial.print(", ");
    }
}

void setup() {
  Serial.begin(9600);
  while(!Serial) {;}
}

void test1() {
  char hmacSecret [16];
  for(int i = 0; i<16; i++){
    hmacSecret[i] = 0;
  }
  char output[32];
  char * s = "foo";

  Serial.println("begining hash test");

  Serial.print("hmacSecret : ");
  print_array(hmacSecret, 16);
  Serial.println(";");

  Serial.print("s : ");
  print_array(s, strlen(s));
  Serial.println(";");


  hashcode.resetHMAC(hmacSecret, 16);
  hashcode.update(s, strlen(s));
  hashcode.finalizeHMAC(hmacSecret, 16, output, 32);

  Serial.print("Result : ");
  print_array(output, 32);
  Serial.println(";");
}

void test2() {
  char hmacSecret [0];
  for(int i = 0; i<16; i++){
    hmacSecret[i] = 0;
  }
  char output[32];
  char * s = "";

  Serial.println("begining hash test");

  Serial.print("hmacSecret : ");
  print_array(hmacSecret, 0);
  Serial.println(";");

  Serial.print("s : ");
  print_array(s, strlen(s));
  Serial.println(";");


  hashcode.resetHMAC(hmacSecret, 16);
  hashcode.update(s, strlen(s));
  hashcode.finalizeHMAC(hmacSecret, 16, output, 32);

  Serial.print("Result : ");
  print_array(output, 32);
  Serial.println(";");
}

void loop() {
    test1();
    test2();
    delay(5000);
}

