


    delay(5000);
}#include <Arduino.h>
 #include "string.h"

 void setup() {
   Serial.begin(9600);
   while(!Serial) {;}
 }

 void loop() {
     Serial.println("HELO");
     char buf[257];

     for(size_t i = 0; i<256; i++) {
         Serial.print("init ");
         Serial.println((int)i);
         buf[i] = (char)i;
     }

     const char range = '~' - '!';
     for(size_t j=0; j<256; j++) {
         buf[j] = ((unsigned char)buf[j] % range) + 33;
         Serial.println(buf[j]);
     }

     buf[256] = 0;
     Serial.println(buf);

