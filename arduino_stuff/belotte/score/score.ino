#include <LiquidCrystal.h>
#include "state.h"
#include "lcd.h"

// STATE values
#define BETTING 0
#define SCORING 1
#define GAME_OVER 2

#define BET_STEP 10
#define MAX_BET 160
#define MIN_BET 80

// COLOR values
#define UNDEFINED 0
#define CLUB 1
#define SPADE 2
#define DIAMOND 3
#define HEART 4

//SCORE_MODIFIER values
#define SURCOINCHE 4
#define COINCHE 2
#define NORMAL 1

// GAMBLER values
#define WE 0
#define THEY 1

// PIN definition
#define BTN_1 2
#define BTN_2 4
#define BTN_3 7
#define BTN_4 8

// LCD settings

// Sound
// TODO : move in SND file
#define SND_PIN 3


void setup() {

  pinMode(SND_PIN, OUTPUT);
  bip();
  
  pullupMode(BTN_1);
  pullupMode(BTN_2);
  pullupMode(BTN_3);
  pullupMode(BTN_4);
  bip(); delay(200);

  initLcd();
  bip(); delay(200);

  initJoystick();
  bip(); delay(200);
  
  Serial.begin(9600);

  initState();
      
  delay(2000);
}

void pullupMode(byte pin) {
  pinMode(pin, INPUT);
  digitalWrite(pin, HIGH);
}

void loop() {
  delay(100);
  if(isResetting()) {
    bipbip();
    swipeMemory();
    setup();
  }
  else if(isEditScore()) {
    editScore();
  }
  if(isBettingTime()) {
    betting();
  } 
  else if(isScoringTime()) {
    playing();
  }
  else if (isGamingOver()){
    gameOver();
  }
  
  printState();
  delay(250);

}

void betting() {
  const byte bet = adjustBet(readBetValue());
  const byte color = readColor();
  const byte scoreModifier = readScoreModifier();
  const byte gambler = readGambler();
  outputBet(BETTING, bet, color, scoreModifier, gambler);
  byte score[roundsArraySize()]; 
  getScore(score);
  byte roundCounter = getRoundCounter();
  outputTotal(score, roundCounter);
  saveBet(bet, color, scoreModifier, gambler);
  validateBet(color);
}

void playing() {
  if(won()){
    bipbip();
    weScore();
  } else if(lost()){
    bipbip();
    theyScore();
  } else {
    // print this in case of reboot
    outputBet(SCORING, getBet(), getColor(), getScoreModifier(), getGambler());
    byte score[roundsArraySize()];
    getScore(score);
    byte roundCounter = getRoundCounter();
    outputTotal(score, roundCounter);
  }
}

void gameOver() {
  byte score[roundsArraySize()]; 
  getScore(score);
  byte roundCounter = getRoundCounter();
  outputTotal(score, roundCounter);
  outputGameOver();
  melody();
}

boolean readScore(byte* score, byte roundCounter, boolean (*totalCallback)(int,int), void (*roundResultCallback)(int, byte, byte, byte, byte, int)){
  int we = 0;
  int they = 0;

  for(int i=0; i<roundCounter; i++){
    const byte slot = i * NB_INDEX;
    const byte gambler = score[slot + I_GAMBLER];
    const byte winner = score[slot + I_WINNER];
    const byte scoreModifier = score[slot + I_SCORE_MODIFIER];
    const byte bet = score[slot + I_BET];
    int marked = 0;
    if(gambler == winner){
      marked = bet * scoreModifier;
    } 
    else {
      marked = 160 * scoreModifier;
    }
   
    if(winner == WE) {
      we = we + marked;
    } 
    else if (winner == THEY) {
      they = they + marked;
    }
    
    roundResultCallback(i, gambler, winner, scoreModifier, bet, marked);

  }

  return totalCallback(we, they);
}

boolean isResetting() {
  return isBtnPressed(BTN_1) && isBtnPressed(BTN_2) && isBtnPressed(BTN_3) && isBtnPressed(BTN_4);
}

////////////////////////////////////////
// Reading betting input setup functions
////////////////////////////////////////

char readBetValue(){
  if(isBtnPressed(BTN_3) && isBtnPressed(BTN_2)){
    bip();
    return BET_STEP;
  }
  if(isBtnPressed(BTN_3) && isBtnPressed(BTN_1)){
    bip();
    return -BET_STEP;
  }
  return 0;
}

byte readColor() {
  byte current = getColor();
  if(isBtnPressed(BTN_4) && isBtnPressed(BTN_1)) {
    bip();
    return current % 4 + 1;
  }
  return current;
}

byte readScoreModifier(){
  byte current = getScoreModifier();
  if(isBtnPressed(BTN_4) && isBtnPressed(BTN_2)) {
    bip();
    if(current == NORMAL) return COINCHE;
    else if(current == COINCHE) return SURCOINCHE;
    else return NORMAL;
  }
  return current;
}

byte readGambler(){
  byte current = getGambler();
  if(isBtnPressed(BTN_4) && isBtnPressed(BTN_3)) {
    bip();
    return (current + 1) % 2;
  }
  return current;
}

void validateBet(byte color) {
  if(color != UNDEFINED && isBtnPressed(BTN_1) && isBtnPressed(BTN_2)){
    bipbip();
    setState(SCORING);
  }
}

/////////////////////////////////////
// Betting setup functional functions
/////////////////////////////////////

boolean isBettingTime(){
  return getState() == BETTING;
}

byte adjustBet(char v) {
  return max(min(getBet() + v, MAX_BET), MIN_BET);
}

void outputBet(byte state, byte bet, byte color, byte scoreModifier, byte gambler) {
  lcdClearLine(0);
  String line1 = String(" ");
  line1 += gamblerAsStr(gambler);
  line1 += " ";
  if(scoreModifier != NORMAL) {
    line1 += scoreModifierAsStr(scoreModifier);
    line1 += " ";  
  }
  line1 += bet;
  line1 += " ";
  getLcd()->write(stateChar(state));
  getLcd()->print(line1);
  getLcd()->write(colorChar(color));
}

byte colorChar(byte color) {
  //nice coincidence ;)
  return color;
}
byte stateChar(byte state) {
  if(state == BETTING) return PAUSE_CHAR;
  if(state == SCORING) return PLAY_CHAR;  
}

char* scoreModifierAsStr(byte scoreModifier) {
  if(scoreModifier == COINCHE) return "CNCH";
  if(scoreModifier == SURCOINCHE) return "SRCNCH";
}
char* colorAsStr(byte color) {
 if(color == HEART) return "coeur";
 if(color == SPADE) return "pique";
 if(color == CLUB) return "trefle";
 if(color == DIAMOND) return "carreau";
 return "???";   
}

char* gamblerAsStr(byte gambler) {
  if(gambler == WE) return "N";
  if(gambler == THEY) return "E";
  return "???";
}

//////////////////////////
// Scoring input functions
//////////////////////////

boolean won() {
  return isBtnPressed(BTN_4) && isBtnPressed(BTN_1);
}

boolean lost() {
  return isBtnPressed(BTN_4) && isBtnPressed(BTN_2);
}

///////////////////////////////
// Scoring functional functions
///////////////////////////////

boolean isScoringTime() {
  return getState() == SCORING;
}

void score(byte winner) {
  writeScore(getGambler(), winner, getBet(), getScoreModifier());
  newRound();
}

void weScore(){
  score(WE);
}

void theyScore(){
  score(THEY);
}

void newRound(){
  byte score[roundsArraySize()]; 
  getScore(score);
  byte roundCounter = getRoundCounter();
  if(isGameOver(score, roundCounter)) {
    setState(GAME_OVER);
  } else {
    resetState();
  }
}

boolean testGameOver(int we, int they) {
  return we > 1000 || they > 1000;
}

void nopRoundResultCallback(int i, byte gambler, byte winner, byte scoreModifier, byte bet, int marked) {
  
}

boolean isGameOver(byte* score, byte roundCounter) {
  return readScore(score, roundCounter, testGameOver, nopRoundResultCallback);   
}

void printState(){
  Serial.println("-------------------------");
  printByte("STATE", getState());
  printByte("BET", getBet());
  printByte("COLOR", getColor());
  printByte("GAMBLER", getGambler());
  printByte("SCORE_MODIFIER", getScoreModifier());
  printByte("ROUND_CNT", getRoundCounter());
  byte score[roundsArraySize()]; 
  getScore(score);
  byte roundCounter = getRoundCounter();
  printScore(score, roundCounter);

  
}

void printByte(char name[], byte v) {
  Serial.print(name);
  Serial.print(" = ");
  Serial.println(v);
}

boolean printTotal(int we, int they) {
  String total = String("Total : WE=");
  total += we;
  total += "/THEY=";
  total += they;
  Serial.println(total);
}

void printRoundResultCallback(int i, byte gambler, byte winner, byte scoreModifier, byte bet, int marked) {
  String line = String("|");
  line += i;
  line += "|gambler=";
  line += gambler;
  line += "|winner=";
  line += winner;
  line += "|modifier=";
  line += scoreModifier;
  line += "|bet=";
  line += bet;
  if(winner == WE){
    line += "|we=+";
    line += marked;
    line += "|they=0";
  } else if(winner == THEY){
    line += "|we=0";
    line += "|they=+";
    line += marked;
  }
  Serial.println(line);
}

void printScore(byte* score, byte roundCounter) {
  readScore(score, roundCounter, printTotal, printRoundResultCallback);  
}

boolean lcdOutputTotal(int we, int they) {
  String total = String("Ns:");
  total += we;
  total += "|Eux:";
  total += they;
  lcdClearLine(1);
  getLcd()->print(total);
  return true;
}

void outputTotal(byte* score, byte roundCounter) {
  readScore(score, roundCounter, lcdOutputTotal, nopRoundResultCallback);
}

void outputGameOver() {
  lcdClearLine(0);
  getLcd()->print("Une autre ?");  
}

boolean isGamingOver(){
  return getState() == GAME_OVER;
}

// HARDWARE FUNCTIONS

boolean isBtnPressed(int btnPin) {
  return ! isPinHigh(btnPin);
}

boolean isPinHigh(int pin){
  return digitalRead(pin) == HIGH;
}
