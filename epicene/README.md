Epicene is a gender agnostic GameBoy emulator that targets human beings 
interested in terminal retro-gaming. The author plans to make it dog
friendly (but definitely never for cats).

# Motivation

My parents never bought me this popular entertaining little piece of hardware
that is the GameBoy. So 20 years later, I decided to make my own.
Also, I never played Pokemon and wanted to give it a try the hard way.

No, no, no, that's not a motivation for a grown up. Try this one :
 
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

## Absolutely necessary

- [Opcodes quick reference](http://www.pastraiser.com/cpu/gameboy/gameboy_opcodes.html)
- [A guide for writing games](http://marc.rawer.de/Gameboy/Docs/GBCPUman.pdf)
- [Pandocs developer guide](http://bgb.bircd.org/pandocs.htm)
- [The cycle accurate Game Boy Doucumentation](https://github.com/AntonioND/giibiiadvance/blob/master/docs/TCAGBD.pdf) is a good complement to pandoc.

## Handy
- [A walk through RealBoy](https://realboyemulator.wordpress.com/)
- [A Javascript implementation explained](http://imrannazar.com/GameBoy-Emulation-in-JavaScript)
- [Yet another Game Boy instruction set](https://gist.github.com/sifton/4471555)
- [Game Boy internals cheat sheet](http://www.chrisantonellis.com/files/gameboy/gb-cribsheet.pdfhttp://www.chrisantonellis.com/files/gameboy/gb-cribsheet.pdf)
- [GameBoy Z80 CPU Opcodes cheat sheet](http://goldencrystal.free.fr/GBZ80Opcodes.pdf)
- [Awake]() is a decompiles ROMS in pseudo code, very useful !!
- [A tutorial](http://gameboy.mongenel.com/asmschool.html) for writing programs on GameBoy
- [Test Roms](http://gbdev.gg8.se/files/roms/blargg-gb-tests/) to test your implementation. Priceless.
- [Z80 syntax](http://www.z80.info/z80syntx.htm) with useful diagrams.
- [Z80 user guide](http://www.zilog.com/appnotes_download.php?FromPage=DirectLink&dn=UM0080&ft=User%20Manual&f=YUhSMGNEb3ZMM2QzZHk1NmFXeHZaeTVqYjIwdlpHOWpjeTk2T0RBdlZVMHdNRGd3TG5Ca1pnPT0=) describes each instruction in length. Not enough example though.
- [Nesdev](http://forum.nesdev.com). I've done numerous search on the forum via Google 'site:"nesdev.com" blabla'.
- [Contains a chart explaining DAA](http://datasheets.chipdb.org/Zilog/Z80/z80-documented-0.90.pdf)
- [The internal workings of video game consoles: The GameBoy](http://www.idt.mdh.se/utbildning/exjobb/files/TR1234.pdf)
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
unused variable or imports
fortement typé : params, return, generics ... 
pas de null

- les génériques sont compliqués

- quand ça compile, on est à peu près sûr que ça marche

- il y a un lien sur chaque erreur avec un exemple typique et une résolution possible. Par [exemple](https://doc.rust-lang.org/error-index.html#E0389).

# journal de bord
Après avoir essayé Guns Riders, je m'attaque à la rom de test.
J'ai un bug et j'ai la flême de chercher son origine.
J'ai implémenté un débugger mais ça donne pas grand chose.
J'essaye d'implémenter l'écran pour voir où j'en suis et redonner le moral
aux troupes

J'ai un sale bug sur pop AF. La solution est [ici](https://forums.nesdev.com/viewtopic.php?f=20&t=12815).
J'ai 12000 bugs sur DAA et j'ai pas de solution
J'ai un sale bug sur 0xF8 ld hl,(sp+n)

J'ai abandonné l'écran.
J'ai perdu trop de temps à essayer d'être générique au niveau des opcodes.

