#include "pitches.h"


// joyeux enfants de la bourgogne
int melody[] = {
  NOTE_RE4, NOTE_SOL4, NOTE_SI4, NOTE_SI4, NOTE_LA4, NOTE_SI4, NOTE_DO5, NOTE_SI4,NOTE_LA4, NOTE_FA4, NOTE_SOL4, NOTE_LA4, NOTE_LA4, NOTE_RE5, NOTE_RE5, NOTE_RE5, NOTE_RE4 
};

// note durations: 4 = quarter note, 8 = eighth note, etc.:
int noteDurations[] = {
  3, 3, 3, 6, 6, 6, 6, 3, 3, 6, 6, 3, 6, 6, 6, 6, 2
};

/*int melody[] = {
  NOTE_SOL5, NOTE_SOL5, NOTE_SOL5, NOTE_LA5, NOTE_SI5, NOTE_LA5,
  NOTE_SOL5, NOTE_SI5, NOTE_LA5, NOTE_LA5, NOTE_SOL5
};

int noteDurations[] = {
  4, 4, 4, 4, 2, 2,
  4, 4, 4, 4, 1
};*/

void setup() {
  
}

void loop() {
  // iterate over the notes of the melody:
  for (int thisNote = 0; thisNote < sizeof(melody); thisNote++) {

    int noteDuration = 1000 / noteDurations[thisNote];
    tone(8, melody[thisNote], noteDuration);

   int pauseBetweenNotes = noteDuration * 1.30;
    delay(pauseBetweenNotes);
    // stop the tone playing:
    noTone(8);
  }
}
