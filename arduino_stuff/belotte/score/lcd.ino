/*
 * VSS : black (-)
 * VDD : red (+)
 * VO : yellow (pot)
 * RS : orang (12)
 * RW : black (-)
 * E : white (11)
 * D7 : green (10)
 * D6: purple (5)
 * D5 : blue (9)
 * D4 : marron (6)
 */
#include <LiquidCrystal.h>
#include "lcd.h"

#define LCD_NB_COLS 16
#define LCD_NB_ROWS 2

#define LCD_RS 12
#define LCD_ENABLE 11
#define LCD_D4 6
#define LCD_D5 9
#define LCD_D6 5
#define LCD_D7 10

LiquidCrystal LCD(LCD_RS, LCD_ENABLE, LCD_D4, LCD_D5, LCD_D6, LCD_D7);

LiquidCrystal* getLcd() {
  return &LCD;
}

void initLcd() {
  getLcd()->begin(LCD_NB_COLS, LCD_NB_ROWS);
  createCustomChars();
  getLcd()->noDisplay();
  getLcd()->noBlink();
  getLcd()->noCursor();
  lcdClearLine(0);
  getLcd()->print("Made in");
  lcdClearLine(1);
  getLcd()->print("Bourgogne");
  getLcd()->display();
}

void createCustomChars() {
  
  byte spadeBitmap[8] =
  {
  B00100,
  B01110,
  B11111,
  B11111,
  B01110,
  B00100,
  B01110,
  B11111
  };
  
  byte clubBitmap[8] =
  {
  B01110,
  B01110,
  B00100,
  B11011,
  B11011,
  B00100,
  B01110,
  B01110
  };
  
  byte diamondBitmap[8] =
  {
  B00000,
  B00100,
  B01110,
  B11111,
  B01110,
  B00100,
  B00000,
  B00000
  };
  
  byte heartBitmap[8] =
  {
  B00000,
  B01010,
  B11111,
  B11111,
  B11111,
  B01110,
  B00100,
  B00000
  };
  
  byte undefinedBitmap[8] =
  {
  B11011,
  B10001,
  B10001,
  B00000,
  B00000,
  B10001,
  B10001,
  B11011
  };
  
  byte playBitmap[8] =
  {
  B11000,
  B11100,
  B11110,
  B11111,
  B11110,
  B11100,
  B11000,
  B10000
  };
  
  byte pauseBitmap[8] =
  {
  B00000,
  B11011,
  B11011,
  B11011,
  B11011,
  B11011,
  B11011,
  B00000
  };

  // !! can only create 8 custom chars !!
  getLcd()->createChar(CLUB_CHAR, clubBitmap);
  getLcd()->createChar(HEART_CHAR, heartBitmap);
  getLcd()->createChar(SPADE_CHAR, spadeBitmap);
  getLcd()->createChar(DIAMOND_CHAR, diamondBitmap);
  getLcd()->createChar(UNDEFINED_CHAR, undefinedBitmap);
  getLcd()->createChar(PLAY_CHAR, playBitmap);
  getLcd()->createChar(PAUSE_CHAR, pauseBitmap);
}

void lcdClearLine(byte lineNum) {
  getLcd()->setCursor(0, lineNum);
  getLcd()->print("                ");
  getLcd()->setCursor(0, lineNum);
}
