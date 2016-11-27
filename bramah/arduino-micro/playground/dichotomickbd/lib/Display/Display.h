#ifndef DISPLAY_H
#define DISPLAY_H

// see https://www.arduino.cc/en/Tutorial/HelloWorld?from=Tutorial.LiquidCrystal

#include <stdio.h>
#include <ctype.h>
#include "Arduino.h"
#include "LiquidCrystal.h"

#define LCD_NB_COLS 16
#define LCD_NB_ROWS 2

#define LCD_RS 4
#define LCD_ENABLE 3
#define LCD_D4 9
#define LCD_D5 8
#define LCD_D6 7
#define LCD_D7 6

class Display {
  public:
    Display();
    ~Display();
    void append(char);
    void append(char *);
    void replace(char);
    void erase();
    char *getLine1();
    void reset();
  private:
    LiquidCrystal *lcd;
    char * line1;
    size_t line1Len;
};

#endif
