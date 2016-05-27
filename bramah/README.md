h1. How to

In order to be able to act as a HID keyboard, the arduino uno little CPU that acts as USB bridge has to be flashed with a keyboard HID firmware. But, with a keyboard HID firmware, we can't send new sketch, so we have to flash it again to the original firmware.

There is two scrips :
flash-keyboard.sh
flash-original.sh

Before launching any of them, we must put the little CPU in flashing mode,
making briefly a bridge between RST and GND. Hard reboot is necessary after
each flash.


h1. Diary

h3. Day 4
Spent some time finding a nice way to have input. I opt for a keypad with
multitap, but I'm too lazy to try anything this evening.
The Keypad library that comes with arduino can support this. There is an
example called DynamicKeypad.ino.

h3. Day 3
I finally managed to generate a password and type it on keyboard.
I have to check that my mapping to hid is correct.

h3. Day 2
Tried to use the Keyboard library but it doesn't work with Uno
Found 2 workarounds :
- A soft usb :
  http://blog.petrockblock.com/2012/05/19/usb-keyboard-with-arduino-and-v-usb-library-an-example/
But I need some electronic components
- Flash the ATMEGA that control the USB. Seems tedious.
  http://mitchtech.net/arduino-usb-hid-keyboard/

Finally, opted for flashing the USB controller.
Now, I have to use http://www.usb.org/developers/hidpage/Hut1_12v2.pdf in order
to output the correct letter. 

h3. Day 1

Had a first POC : can generate a SHA256 and Base64 the output
