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
- [Another developer guide](http://bgb.bircd.org/pandocs.htm)
- [A Javascript implementation explained](http://imrannazar.com/GameBoy-Emulation-in-JavaScript)
- [Yet another Game Boy instruction set](https://gist.github.com/sifton/4471555)
- [Game Boy internals cheat sheet](http://www.chrisantonellis.com/files/gameboy/gb-cribsheet.pdfhttp://www.chrisantonellis.com/files/gameboy/gb-cribsheet.pdf)
- [GameBoy Z80 CPU Opcodes cheat sheet](http://goldencrystal.free.fr/GBZ80Opcodes.pdf)
- [Awake]() is a decompiles ROMS in pseudo code, very useful !!
- [A tutorial](http://gameboy.mongenel.com/asmschool.html) for writing programs on GameBoy

# Struggling with Rust :

- Recursive generic type :

Here use case requires a type which requires a type which ....

```rust
struct UseCase<F: Fn(ComputerUnit, UseCase) -> ()> {
    program: Program,
    assertions: F
}
```

- Most of the time, be straight forward. Avoid abstractions. Be mutable.

- Rust encompass mutability. It enables developer to reason about mutable code.

- Mutability is not at data level, it's at access (pointer) level and it's 
clever.

- The compilator is a jewel. It helps a lot. For instance, 
missing else : an if alone should return ().
forgot branch in switch case
dead code detection
unused variable
fortement typé : params, return, generics ... 
pas de null

- les génériques sont compliqués

- quand ça compile, on est à peu près sûr que ça marche


