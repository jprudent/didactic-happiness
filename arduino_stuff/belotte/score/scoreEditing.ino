#define KEEP 0
#define DELETE 1

boolean isEditScore() {
  return isJoystickZ();
}

boolean nopTotal(int we, int they) {
  return true;
}

int DISPLAYING_ROUND_CNT = -1;
int EDIT_ACTION = KEEP;

void outputRoundEditing(int i, byte gambler, byte winner, byte scoreModifier, byte bet, int marked) {
  if(i == DISPLAYING_ROUND_CNT) {
    String msg = String("");
    msg += i+1;
    msg += "/";
    msg += getRoundCounter();
    msg += ": ";
    if(winner == WE) {
      msg += "Ns ";
    } else {
      msg += "Eux ";
    }
    msg+=marked;
    Serial.println(msg);
    lcdClearLine(0);
    getLcd()->print(msg);
    
    String action = String("Supprimer : ");
    if(EDIT_ACTION == KEEP) {
      action += "[ ]";
    } else {
      action += "[X]";
    }
    lcdClearLine(1);
    getLcd()->print(action);
  }
}

void rewriteRound(int i, byte gambler, byte winner, byte scoreModifier, byte bet, int marked) {
  Serial.println(String("i=") + i + ",DISPLAYING_ROUND_CNT = " + DISPLAYING_ROUND_CNT);
  if(i != DISPLAYING_ROUND_CNT) {
    writeScore(gambler, winner, bet, scoreModifier);
  }
}
 

void editScore() {
  bipbip();
  lcdClearLine(1);
  lcdClearLine(0);
  getLcd()->println("Edit");
  
  DISPLAYING_ROUND_CNT = 0;
  EDIT_ACTION = KEEP;

  if(getRoundCounter() > 0) {
    boolean stop = false;
    byte score[roundsArraySize()]; 
    getScore(score);
    byte roundCounter = getRoundCounter();
    while(!stop) {
      if(isJoystickRight()){
        DISPLAYING_ROUND_CNT = min(DISPLAYING_ROUND_CNT + 1, getRoundCounter() -1);
        EDIT_ACTION = KEEP;
      } else if (isJoystickLeft()) { 
        DISPLAYING_ROUND_CNT = max(DISPLAYING_ROUND_CNT - 1, 0);
        EDIT_ACTION = KEEP;
      } else if (isJoystickUp() || isJoystickDown()) {
        if(EDIT_ACTION == KEEP) EDIT_ACTION = DELETE;
        else EDIT_ACTION = KEEP;
      }
      readScore(score, roundCounter, nopTotal, outputRoundEditing);
      delay(300);
      stop = isJoystickZ();
    }

    if (EDIT_ACTION == DELETE) {
      resetRoundCounter();
      readScore(score, roundCounter, nopTotal, rewriteRound);
    }
  }
  delay(2000);
}

