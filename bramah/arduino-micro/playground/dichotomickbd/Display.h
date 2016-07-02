#ifndef DISPLAY_H
#define DISPLAY_H

// see https://www.arduino.cc/en/Tutorial/HelloWorld?from=Tutorial.LiquidCrystal

#include <stdio.h>
#include <ctype.h>
#include <Arduino.h>
#include <LiquidCrystal.h>

#define LCD_NB_COLS 16
#define LCD_NB_ROWS 2

#define LCD_RS 12
#define LCD_ENABLE 11
#define LCD_D4 10
#define LCD_D5 9
#define LCD_D6 8
#define LCD_D7 7

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
