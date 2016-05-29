#include <LiquidCrystal.h>

#include "Display.h"
#include <Keypad.h>
Display::Display() {
  pinMode(LCD_RS, OUTPUT);
  pinMode(LCD_ENABLE, OUTPUT);
  pinMode(LCD_D4, OUTPUT);
  pinMode(LCD_D5, OUTPUT);
  pinMode(LCD_D6, OUTPUT);
  pinMode(LCD_D7, OUTPUT);
  this->lcd = new LiquidCrystal(LCD_RS, LCD_ENABLE, LCD_D4, LCD_D5, LCD_D6, LCD_D7);
  this->lcd->begin(LCD_NB_COLS, LCD_NB_ROWS);
  this->lcd->cursor();
  this->lcd->blink();
  this->line1 = (char *) malloc((LCD_NB_COLS + 1) * sizeof(char));
  for (int i = 0; i <= 16; i++) {
    line1[i] = '\0';
  }
  line1Len = 0;
}

Display::~Display() {
  free(this->lcd);
  free(this->line1);
}

void Display::append(char c) {
  this->replace(c);
  if (line1Len < LCD_NB_COLS) {
    line1Len = line1Len + 1;
  }
}

void Display::replace(char c) {
  this->lcd->clear();
  line1[line1Len] = c;
  this->lcd->print(line1);
  Serial.println(line1Len);
  Serial.println(String("\"") + line1 + "\"");
}

