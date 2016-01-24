#include "state.h"
#include <EEPROM.h>


#define MAGIC_ID 21

// Global state is persisted in EEPROM

// application id address
#define MAGIC_ID_ADDR 0

#define STATE_ADDR 1 // BETTING, SCORING, GAME_OVER
#define BET_ADDR 2
#define COLOR_ADDR 3 // HEART, CLUB, SPADE, DIAMOND
#define GAMBLER_ADDR 4 // WE, THEY
#define SCORE_MODIFIER_ADDR 5 // NORMAL, COINCHE, SURCOINCHE

#define ROUND_COUNTER_ADDR 6
//array of rounds. Size is determined with ROUND_COUNTER.
#define ROUNDS_ARRAY_ADDR 7

// GLOBAL STATE
///////////////

byte getState() {
  return EEPROM.read(STATE_ADDR);
}

byte getBet() {
  return EEPROM.read(BET_ADDR);
}

byte getColor() {
  return EEPROM.read(COLOR_ADDR);
}

byte getGambler() {
  return EEPROM.read(GAMBLER_ADDR);
}

byte getScoreModifier() {
  return EEPROM.read(SCORE_MODIFIER_ADDR);
}

byte getRoundCounter() {
  return EEPROM.read(ROUND_COUNTER_ADDR);
}

byte getNbRounds() {
  return getRoundCounter();
}

int roundsArraySize() {
  return getRoundCounter() * NB_INDEX;
}

void getScore(byte* dest) {
  for(int i=0; i<roundsArraySize(); i++) {
    const byte eepromAddr = ROUNDS_ARRAY_ADDR + i;
    dest[i] = EEPROM.read(eepromAddr);
  }
}

void saveBet(byte bet, byte color, byte scoreModifier, byte gambler) {
  EEPROM.write(BET_ADDR, bet);
  EEPROM.write(COLOR_ADDR, color);
  EEPROM.write(SCORE_MODIFIER_ADDR, scoreModifier);
  EEPROM.write(GAMBLER_ADDR, gambler);
}

void resetRoundCounter() {
  EEPROM.write(ROUND_COUNTER_ADDR, 0);
}

void resetState() {
  setState(BETTING);
  saveBet(MIN_BET, UNDEFINED, NORMAL, WE);
}

void setState(byte newState) {
  EEPROM.write(STATE_ADDR, newState);
}

void writeScore(byte gambler, byte winner, byte bet, byte scoreModifier){
  const byte slot = ROUNDS_ARRAY_ADDR + (getRoundCounter() * NB_INDEX);
  EEPROM.write(slot + I_GAMBLER, gambler);
  EEPROM.write(slot + I_WINNER, winner);
  EEPROM.write(slot + I_BET, bet);
  EEPROM.write(slot + I_SCORE_MODIFIER, scoreModifier);
  EEPROM.write(ROUND_COUNTER_ADDR, getRoundCounter() + 1);
}

boolean sameMagicId() {
  return EEPROM.read(MAGIC_ID_ADDR) == MAGIC_ID;
}

void writeMagicId() {
  EEPROM.write(MAGIC_ID_ADDR, MAGIC_ID);
}

void initState() {
  if(!sameMagicId() || getState() == GAME_OVER) {
    resetState();
    resetRoundCounter();
    writeMagicId();
  }
}

void swipeMemory() {
  EEPROM.write(MAGIC_ID_ADDR, 0);
}

