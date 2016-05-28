#include "MultitapKeypad.h"
#include <Keypad.h>

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
//Keypad numpad( makeKeymap(numberKeys), rowPins, colPins, sizeof(rowPins), sizeof(colPins) );
//Keypad ltrpad( makeKeymap(alphaKeys), rowPins, colPins, sizeof(rowPins), sizeof(colPins) );


unsigned long startTime;
const byte ledPin = 13;

char key;

static char virtKey = NO_KEY;      // Stores the last virtual key press. (Alpha keys only)
static char physKey = NO_KEY;      // Stores the last physical key press. (Alpha keys only)
static char buildStr[12];
static byte buildCount;
static byte pressCount;

//static byte kpadState

// Take care of some special events.
void MultitapKeypad::swOnState(char key, KeyState kpadState) {
  switch ( kpadState ) {
    case PRESSED:
      if (key == '#') {
        break;
      }
      else if (isalpha(key)) {              // This is a letter key so we're using the letter keymap.
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
      }
      else if (isdigit(key) || key == ' ' || key == '.') {
        Serial.print(key);
      }
      this->onRotateKeyCb(virtKey);
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
      else  {           
        this->onConfirmKeyCb((isalpha(key)) ? virtKey : key);
      }
      break;
  }  // end switch-case
}// end switch on state function

MultitapKeypad::MultitapKeypad(void (*onRotateKeyCb)(char), void (*onConfirmKeyCb)(char)) {
  this->numpad = new Keypad( makeKeymap(numberKeys), rowPins, colPins, sizeof(rowPins), sizeof(colPins) );
  this->ltrpad = new Keypad( makeKeymap(alphaKeys), rowPins, colPins, sizeof(rowPins), sizeof(colPins) );
  this->onRotateKeyCb = onRotateKeyCb;
  this->onConfirmKeyCb = onConfirmKeyCb;
};

void MultitapKeypad::setup() {
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW);                 // Turns the LED on.
  ltrpad->begin( makeKeymap(alphaKeys) );
  numpad->begin( makeKeymap(numberKeys) );
  ltrpad->setHoldTime(500);                   // Default is 1000mS
  numpad->setHoldTime(500);                   // Default is 1000mS
  Serial.println("loaded");
}

MultitapKeypad::~MultitapKeypad() {
  free(this->numpad);
  free(this->ltrpad);
}

void MultitapKeypad::onKeyChange(Keypad keypad) {
  for (int i = 0; i < LIST_MAX; i++) {
    this->swOnState(keypad.key[i].kchar, keypad.key[i].kstate);
  }
}

char MultitapKeypad::getKeys() {
  if (alpha) {
    if (ltrpad->getKeys()) {
      onKeyChange(*this->ltrpad);
    }
  }
  else {
    if (numpad->getKeys()) {
      onKeyChange(*this->numpad);
    }
  }

  if (alpha && millis() - startTime > 100) {       // Flash the LED if we are using the letter keymap.
    digitalWrite(ledPin, !digitalRead(ledPin));
    startTime = millis();
  }
}
