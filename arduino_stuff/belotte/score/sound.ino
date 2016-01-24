#include "pitches.h"

// joyeux enfants de la bourgogne
int MELODY[] = {
  NOTE_RE4, NOTE_SOL4, NOTE_SI4, NOTE_SI4, NOTE_LA4, NOTE_SI4, NOTE_DO5, NOTE_SI4,NOTE_LA4, NOTE_FA4, NOTE_SOL4, NOTE_LA4, NOTE_LA4, NOTE_RE5, NOTE_RE5, NOTE_RE5, NOTE_RE4 
};

// note durations: 4 = quarter note, 8 = eighth note, etc.:
int MELODY_DURATIONS[] = {
  3, 3, 3, 6, 6, 6, 6, 3, 3, 6, 6, 3, 6, 6, 6, 6, 2
};

void bip(){
  tone(SND_PIN, 440, 100);
}

void bipbip(){
  tone(SND_PIN, 220, 100);
  delay(200);
  tone(SND_PIN, 660, 100);
  delay(200);
  tone(SND_PIN, 220, 100);
}

void melody() {
  // iterate over the notes of the melody:
  for (int thisNote = 0; thisNote < sizeof(MELODY); thisNote++) {

    int noteDuration = 1000 / MELODY_DURATIONS[thisNote];
    tone(SND_PIN, MELODY[thisNote], noteDuration);

   int pauseBetweenNotes = noteDuration * 1.30;
    delay(pauseBetweenNotes);
    // stop the tone playing:
    noTone(SND_PIN);
  }
}
