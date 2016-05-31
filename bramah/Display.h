#ifndef DISPLAY_H
#define DISPLAY_H

// see https://www.arduino.cc/en/Tutorial/HelloWorld?from=Tutorial.LiquidCrystal

#include <stdio.h>
#include <ctype.h>
#include <Arduino.h>

#define LCD_NB_COLS 16
#define LCD_NB_ROWS 2

#define LCD_RS A4
#define LCD_ENABLE A5
#define LCD_D4 A0
#define LCD_D5 A1
#define LCD_D6 A2
#define LCD_D7 A3

class Display {
  public:
    Display();
    ~Display();
    void append(char);
    void replace(char);
  private:
    LiquidCrystal *lcd;
    char * line1;
    size_t line1Len;
};

#endif
