Epicene is a gender agnostic GameBoy emulator that targets human beings 
interested in terminal retro-gaming. The author plans to make it dog
friendly (but definitely never for cats).

# Motivation

My parents never bought me this popular entertaining little piece of hardware
that is the GameBoy. So 20 years later, I decided to make my own.
Also, I never played Pokemon and wanted to give it a try the hard way.

No, no, that's not a motivation for a grown up. Here is the truth :
 
As a java developer, I have been too much time away from the hardware.
Cool assembly hacks and hardware design is something my life is missing for.
I'm slowly diving in the bare metal by writing emulators.
I made a first attempt in emulation world with my CHIP-8 emulator. But this
is not a real machine, more like a virtual machine. I wanted something that 
produced a more attractive result. I decided GameBoy, because it's the first
thought I had. After googling for 9 seconds I found a lot of resources 
about the machine guts. 

Also, I'm trying to learn the Rust langage. And an emulator seemed the perfect
little pet project for this purpose.
 
# Technical Resources

- [Opcodes quick reference](http://www.pastraiser.com/cpu/gameboy/gameboy_opcodes.html)
- [A guide for writing games](http://marc.rawer.de/Gameboy/Docs/GBCPUman.pdf)
- [A walk through RealBoy](https://realboyemulator.wordpress.com/)