#include <EEPROM.h>



int i = 0;
void setup() {
  Serial.begin(9600);
  while(!Serial){;}
  HmacSecretData hmacSecretData;
  hmacSecretData = readSecret(hmacSecretData);
  Serial.println("magic : ");
  Serial.println(hmacSecretData.magicNumber);
  hmacSecretData.magicNumber = 222222;
  writeSecret(hmacSecretData);
  Serial.println(hmacSecretData.magicNumber);
}

void loop() {

  HmacSecretData hmacSecretDataRead;
  hmacSecretDataRead = readSecret(hmacSecretDataRead);
  Serial.println("read magic : ");
  Serial.println(i++);
  Serial.println(hmacSecretDataRead.magicNumber);
  long int magicLong;
  EEPROM.get(21, magicLong);
  Serial.println(magicLong);
  delay(1000);
}

// #include <EEPROM.h>
//
// struct MyObject {
//   float field1;
//   byte field2;
//   char name[10];
// };
//
// void setup() {
//
//   Serial.begin(9600);
//   while (!Serial) {
//     ; // wait for serial port to connect. Needed for native USB port only
//   }
//
//   float f = 123.456f;  //Variable to store in EEPROM.
//   int eeAddress = 0;   //Location we want the data to be put.
//
//
//   //One simple call, with the address first and the object second.
//   EEPROM.put(eeAddress, f);
//
//   Serial.println("Written float data type!");
//
//   /** Put is designed for use with custom structures also. **/
//
//   //Data to store.
//   MyObject customVar = {
//     3.14f,
//     65,
//     "Working!"
//   };
//
//   eeAddress += sizeof(float); //Move address to the next byte after float 'f'.
//
//   EEPROM.put(eeAddress, customVar);
//   Serial.print("Written custom data type! \n\nView the example sketch eeprom_get to see how you can retrieve the values!");
//
//   MyObject reading;
//   EEPROM.get(eeAddress, reading);
//
//   Serial.println( "Read custom object from EEPROM: " );
//   Serial.println( customVar.field1 );
//   Serial.println( customVar.field2 );
//   Serial.println( customVar.name );
// }
//
// void loop() {
//   /* Empty loop */
// }
