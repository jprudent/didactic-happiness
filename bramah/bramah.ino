#include "SHA256.h"
#include "Base64.h"
#include <Keypad.h>
#include <ctype.h>

/**
 * Password generator stuff
 */
const char* PASSWORD = "passw";
const char* SITE = "gmail.com";
const size_t HLEN = 32;

SHA256 hashcode;

void debug(const char * s) {
  Serial.println(s);
}

// Compute the hash of s1 + s2
// output: the output string of size HLEN
void hash(char * output, const char * s1, const char * s2) {
  hashcode.reset();
  hashcode.update(s1, strlen(s1));
  hashcode.update(s2, strlen(s2));
  hashcode.finalize(output, HLEN);
}

// Encode input in ASCII readable output
// output must have HLEN size
// output may not be reversable to input
void encode_to_ascii(char * output, char * input) {
  const int b64l = base64_enc_len(HLEN);
  char b64encoded[b64l];
  base64_encode(b64encoded, input, HLEN);
  memcpy(output, b64encoded, HLEN);
}

// output: the output string of size HLEN
void generate_password(char * output, const char * master_password, const char * memo) {
  char sha[HLEN];
  hash(sha, master_password, memo);
  encode_to_ascii(output, sha);
}

/**
 * HID keyboard stuff
 */
const size_t KBD_BUF_SIZE = 8;
const char KEY_LEFT_SHIFT = 0x02;
const char KEY_QUESTION_MARK = 0x38;
const char * SIMPLE_KEYS = "abcdefghijklmnopqrstuvwxyz1234567890\x0A\x1B\x08\t -=[]\\\0;'`,./\0";
const char * SHIFT_KEYS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()\0\0\0\0\0_+{}|\0:\"~<>?";
const size_t MODIFIER_IDX = 0;
const size_t KEY1_IDX = 2;

size_t index_of(const char ascii_char, const char * s) {
  for (size_t i = 0; i < strlen(s); i++) {
    if (s[i] == ascii_char) {
      return i;
    }
  }
  return -1;
}

char to_hid_page_7(uint8_t * keyboard_buffer, const char ascii_char) {
  size_t index = index_of(ascii_char, SIMPLE_KEYS);
  if (index != -1) {
    keyboard_buffer[MODIFIER_IDX] = 0;
    keyboard_buffer[KEY1_IDX] = index + 0x04; // because a = 4
  } else {
    index = index_of(ascii_char, SHIFT_KEYS);
    keyboard_buffer[MODIFIER_IDX] = KEY_LEFT_SHIFT;
    if (index != -1) {
      keyboard_buffer[KEY1_IDX] = index + 0x04; // because a = 4
    } else {
      keyboard_buffer[KEY1_IDX] = KEY_QUESTION_MARK;
    }
  }
}

void write_keyboard(uint8_t * keyboard_buffer) {
  Serial.write(keyboard_buffer, KBD_BUF_SIZE);
}

void press_key(uint8_t * keyboard_buffer, const char ascii_char) {
  to_hid_page_7(keyboard_buffer, ascii_char);
  write_keyboard(keyboard_buffer);
}

void release_key(uint8_t * keyboard_buffer) {
  for (int i = 0; i < KBD_BUF_SIZE; i++) {
    keyboard_buffer[i] = 0;
  }
  write_keyboard(keyboard_buffer);
}

void type_on_keyboard(char * input) {
  uint8_t keyboard_buffer[KBD_BUF_SIZE] = { 0 };
  for (int i = 0; i < strlen(input); i++) {
    press_key(keyboard_buffer, input[i]);
    release_key(keyboard_buffer);
  }
}

/**
 * Keypad input stuff
 */
const byte ROWS = 4; //four rows
const byte COLS = 3; //three columns
// Define the keymaps.  The blank spot (lower left) is the space character.
char alphaKeys[ROWS][COLS] = {
  { 'a', 'd', 'g' },
  { 'j', 'm', 'p' },
  { 's', 'v', 'y' },
  { ' ', '.', '#' }
};

char numberKeys[ROWS][COLS] = {
  { '1', '2', '3' },
  { '4', '5', '6' },
  { '7', '8', '9' },
  { ' ', '0', '#' }
};

boolean alpha = false;   // Start with the numeric keypad.

byte rowPins[ROWS] = {9, 8, 7, 6}; //connect to the row pinouts of the keypad
byte colPins[COLS] = {5, 4, 3}; //connect to the column pinouts of the keypad

// Create two new keypads, one is a number pad and the other is a letter pad.
Keypad numpad( makeKeymap(numberKeys), rowPins, colPins, sizeof(rowPins), sizeof(colPins) );
Keypad ltrpad( makeKeymap(alphaKeys), rowPins, colPins, sizeof(rowPins), sizeof(colPins) );


unsigned long startTime;
const byte ledPin = 13;

void setupKeypad() {
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW);                 // Turns the LED on.
  ltrpad.begin( makeKeymap(alphaKeys) );
  numpad.begin( makeKeymap(numberKeys) );
  ltrpad.addEventListener(keypadEvent_ltr);  // Add an event listener.
  ltrpad.setHoldTime(500);                   // Default is 1000mS
  numpad.addEventListener(keypadEvent_num);  // Add an event listener.
  numpad.setHoldTime(500);                   // Default is 1000mS
}

char key;

void loopKeypad() {

    if( alpha )
        key = ltrpad.getKey( );
    else
        key = numpad.getKey( );

    if (alpha && millis()-startTime>100) {           // Flash the LED if we are using the letter keymap.
        digitalWrite(ledPin,!digitalRead(ledPin));
        startTime = millis();
    }
}

static char virtKey = NO_KEY;      // Stores the last virtual key press. (Alpha keys only)
static char physKey = NO_KEY;      // Stores the last physical key press. (Alpha keys only)
static char buildStr[12];
static byte buildCount;
static byte pressCount;

static byte kpadState;

// Take care of some special events.

void keypadEvent_ltr(KeypadEvent key) {
    // in here when in alpha mode.
    kpadState = ltrpad.getState( );
    swOnState( key );
} // end ltrs keypad events

void keypadEvent_num( KeypadEvent key ) {
    // in here when using number keypad
    kpadState = numpad.getState( );
    swOnState( key );
} // end numbers keypad events

void swOnState( char key ) {
    switch( kpadState ) {
        case PRESSED:
            if (isalpha(key)) {              // This is a letter key so we're using the letter keymap.
                if (physKey != key) {        // New key so start with the first of 3 characters.
                    pressCount = 0;
                    virtKey = key;
                    physKey = key;
                }
                else {                       // Pressed the same key again...
                    virtKey++;                   // so select the next character on that key.
                    pressCount++;                // Tracks how many times we press the same key.
                }
                    if (pressCount > 2) {    // Last character reached so cycle back to start.
                        pressCount = 0;
                        virtKey = key;
                    }
                    Serial.print(virtKey);   // Used for testing.
                }
                if (isdigit(key) || key == ' ' || key == '.')
                    Serial.print(key);
                if (key == '#')
                    Serial.println();
                break;

        case HOLD:
            if (key == '#')  {               // Toggle between keymaps.
                if (alpha == true)  {        // We are currently using a keymap with letters
                    alpha = false;           // Now we want a keymap with numbers.
                    digitalWrite(ledPin, LOW);
                }
                else  {                      // We are currently using a keymap with numbers
                    alpha = true;            // Now we want a keymap with letters.
                }
            }
            else  {                          // Some key other than '#' was pressed.
                buildStr[buildCount++] = (isalpha(key)) ? virtKey : key;
                buildStr[buildCount] = '\0';
                Serial.println();
                Serial.println(buildStr);
            }
            break;

        case RELEASED:
            if (buildCount >= sizeof(buildStr))  buildCount = 0;  // Our string is full. Start fresh.
            break;
    }  // end switch-case
}// end switch on state function

/**
 * Setup & loop
 */
void setup() {
  Serial.begin(9600);
  setupKeypad();
  delay(1000);
}

void loop() {
  loopKeypad();
  //char output[HLEN + 1];
  //generate_password(output, PASSWORD, SITE);
  //output[HLEN] = 0;
  //type_on_keyboard(output);
  //delay(2000);
}
