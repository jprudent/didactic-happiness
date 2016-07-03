#include <LiquidCrystal.h>

#include "Display.h"

void zeroes(char *s, size_t len) {
  for (int i = 0; i <= len; i++) {
    s[i] = '\0';
  }
}

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
  zeroes(this->line1, LCD_NB_COLS + 1);
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

void Display::append(char *s) {
  for(int i=0; i<strlen(s); i++) {
    this->append(s[i]);
  }
}

void Display::replace(char c) {
  this->lcd->clear();
  line1[line1Len] = c;
  this->lcd->print(line1);
  this->lcd->setCursor(line1Len, 0);
}

void Display::erase() {
  this->replace('\0');
  if (line1Len > 0) {
    line1Len = line1Len - 1;
  }
  this->replace('\0');
}

char* Display::getLine1() {
  return this->line1;
}

void Display::reset() {
  zeroes(this->line1, LCD_NB_COLS + 1);
  line1Len = 0;
  this->lcd->clear();
}
