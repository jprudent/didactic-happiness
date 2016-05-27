#include <Keypad.h>
#include <ctype.h>
//#include "HidKeyboard.h"
#include "PasswordGenerator.h"

/**
 * Password generator stuff
 */
const char* PASSWORD = "passw";
const char* SITE = "gmail.com";

void debug(const char * s) {
    Serial.println(s);
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
    //loopKeypad();
    PasswordGenerator pwdgen;
    char output[HLEN + 1];
    pwdgen.generate_password(output, PASSWORD, SITE);
    Serial.println(output);
    //type_on_keyboard(output);
    //delay(2000);
    //HidKeyboard kbd;
    //kbd.type_on_keyboard("bourgogne ");
    delay(2000);
}
