# Motivations

I made a first proof of concept of Bramah with what I had, namely an Arduino Uno board, a shitty keypad, and a classic LCD display. This poc had several limitations :

- Not really mobile.
- Keypad usability was awful
- With enough horse power (or GPUs) and a single leak of one of the password, an attacker could guess the master password and forge passwords for any website an behalf of Bramah's owner.
- Developer feedback was not so great because we had to constantly reburn the _ATmega16u2_ to act as an _USB HID_ or _Serial to USB converter_.

The purpose of this project is to rewrite _Bramah_ from scratch and make a handful prototypal device.

Since the proof of concept, I gathered useful knowledge in the area of Arduino boards and cryptography.

At that time, I thought I had an absolutely original idea, but I googled for the wrong terms. I found quite but not so similar projects. And .. I still think that building my own device is fun enough to continue the Bramah project.

# The quite but not so similar projects

Sometimes you just don't find stuff on the internet (or maybe your subconscient doesn't want you to find it). The only thing I had to do to find similar project was going [here](https://hackaday.io/search?term=password).

- The most impressive project is the [Mooltipass](https://www.themooltipass.com/). This project came in production mode and cost around 100$. The thing I like the most about it is the use of a smart card.

# Arduino micro

The Arduino micro board ships an _ATmega16u2_ chip. What is really nice is that this boards can act as an _USB HID_ ***and*** a _Serial to USB converter_ at the same time.
