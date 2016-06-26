#!/bin/zsh

echo "erasing"
sleep 2
sudo dfu-programmer atmega16u2 erase
echo "flashing"
sleep 2
sudo dfu-programmer atmega16u2 flash --debug 1 Arduino-keyboard-0.3.hex
echo "resetting"
sleep 2
sudo dfu-programmer atmega16u2 reset
