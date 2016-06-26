// sorry for that, i didn't mean to make your eyes bleeding.

#include "MultitapKeypad.h"
#include <Keypad.h>

const byte ROWS = 4; //four rows
const byte COLS = 4; //three columns
// Define the keymaps.  The blank spot (lower left) is the space character.
const char alphaKeys[ROWS][COLS] = {
  { 'a', 'd', 'g', '\b' },
  { 'j', 'm', 'p', '\b' },
  { 's', 'v', 'y', '\n' },
  { ' ', '.', '#', '\n' }
};

const char numberKeys[ROWS][COLS] = {
  { '1', '2', '3', '\b' },
  { '4', '5', '6', '\b' },
  { '7', '8', '9', '\n' },
  { ' ', '0', '#', '\n' }
};

byte rowPins[ROWS] = {9, 8, 7, 6}; //connect to the row pinouts of the keypad
byte colPins[COLS] = {5, 4, 3, 2}; //connect to the column pinouts of the keypad

static char virtKey = NO_KEY;      // Stores the last virtual key press. (Alpha keys only)
static char physKey = NO_KEY;      // Stores the last physical key press. (Alpha keys only)
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
      else {
        virtKey = key;
      }
      this->onRotateKeyCb(virtKey);
      break;

    case HOLD:
      if (key == '#')  {               // Toggle between keymaps.
        if (this->currentKeypad == this->letterKeypad)  {        // We are currently using a keymap with letters
          this->currentKeypad = this->numberKeypad;
        }
        else  {                      // We are currently using a keymap with numbers
          this->currentKeypad = this->letterKeypad;            // Now we want a keymap with letters.
        }
      }
      else  {
        this->onConfirmKeyCb((isalpha(key)) ? virtKey : key);
      }
      break;
  }  // end switch-case
}// end switch on state function

MultitapKeypad::MultitapKeypad(
        void (*onRotateKeyCb)(char),
        void (*onConfirmKeyCb)(char)) {
  this->numberKeypad = new Keypad(makeKeymap(numberKeys), rowPins, colPins, sizeof(rowPins), sizeof(colPins));
  this->letterKeypad = new Keypad(makeKeymap(alphaKeys), rowPins, colPins, sizeof(rowPins), sizeof(colPins));
  this->currentKeypad = numberKeypad;
  this->onRotateKeyCb = onRotateKeyCb;
  this->onConfirmKeyCb = onConfirmKeyCb;
  letterKeypad->begin( makeKeymap(alphaKeys) );
  numberKeypad->begin( makeKeymap(numberKeys) );
  letterKeypad->setHoldTime(500);                   // Default is 1000mS
  numberKeypad->setHoldTime(500);                   // Default is 1000mS
};

MultitapKeypad::~MultitapKeypad() {
  free(this->numberKeypad);
  free(this->letterKeypad);
}

void MultitapKeypad::onKeyChange(Keypad keypad) {
  for (int i = 0; i < LIST_MAX; i++) {
    this->swOnState(keypad.key[i].kchar, keypad.key[i].kstate);
  }
}

void MultitapKeypad::readKeypad() {
  if(this->currentKeypad->getKeys()) {
    onKeyChange(*this->currentKeypad);
  }
}
