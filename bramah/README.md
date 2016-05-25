h1. How to


h1. Diary
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
