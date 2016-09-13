# cheap-hate

A depressing Clojure interpreter for the
[CHIP-8](https://en.wikipedia.org/wiki/CHIP-8) Virtual Machine.

## Gallery

Here is BRIX. The best game ever made on this platform !
The player is really mediocre, he already lost 2
lives breaking only 3 bricks.


    █ █ █                                                  ████ ████
                                                           █  █    █
                                                           █  █ ████
                                                           █  █    █
                                                           ████ ████

    ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███

    ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███

    ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███

    ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███

    ███ ███ ███ ███ ███ ███ ███     ███ ███ ███ ███ ███ ███ ███ ███

    ███ ███ ███         ███ ███ ███ ███ ███ ███ ███ ███ ███ ███ ███
                      █













            ██████




Here is 15PUZZLE the most boring game ever invented.


                           ████   █  ████ ████
                           █     ██     █    █
                           ████   █  ████ ████
                              █   █  █       █
                           ████  ███ ████ ████

                           ████ ████ ████ █  █
                           █  █ █       █ █  █
                           ████ ████   █  ████
                              █ █  █  █      █
                           ████ ████  █      █

                           ████      ███  ████
                           █  █      █  █ █  █
                           ████      ███  ████
                           █  █      █  █ █  █
                           █  █      ███  ████

                           ███  ████ ████ ████
                           █  █ █    █    █
                           █  █ ████ ████ █
                           █  █ █    █    █
                           ███  ████ █    ████


This is called BLINKY. Now put 3 meters between you and your screen.
 See? It's a Pacman clone.

    ███████████████████████████████ ███████████████████████████████
    █                             █ █                             █
    █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █
    █                             █ █                             █
    █ █ ███████ █ ███ █ ███████ █ ███ █ ███████ █ ███ █ ███████ █ █
    █   █         █ █         █         █         █ █         █   █
    █ █ █ █   █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █   █ █ █ █
    █   █         █ █         █         █         █ █         █   █
    █ █ █ █ ███████████████ █ ███████████ █ ███████████████ █ █ █ █
    █                 █                         █                 █
    █ █ █ █ █ █ █ █ █ █ █ █ █ █   █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █
    █ █               █                         █                 █
    ██ ████████████ █ █ █ █████   ███ █ █████ █ █ █ ███████████ █ █
    █████         █       █               ███       █         █   █
    █ █ █ █ █ █ █ █ █ █ █ █ █            ██ █ █ █ █ █ █ █ █ █ █ █ █
                  █       █                ██       █
        █ █ ███ █ ███ █ ███ █ ████████████ ████ █ ███ █ ███ █ █
                              █         ████
    █ █ █ █ █ █ █ █ █ █ █ █ █ █████ █████ █ █ █ █ █ █ █ █ █ █ █ █ █
    █   █                         █ █                         █   █
    █ █ ███████ █ █████████ █ █ █ █ █ █ █ █ █████████ █ ███████ █ █
    █         █   █       █       █ █       █       █   █         █
    █ █ █   █ █ █ █████████████ █ ███ █ █████████████ █ █ █   █ █ █
    █         █                                         █         █
    █ █ ███ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ ███ █ █
    █   █ █   █                                         █   █ █   █
    █ █ ███ █ ███████████ █ ███ █ ███ █ ███ █ ███████████ █ ███ █ █
    █                       █ █         █ █                       █
    █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █ █
    █                       █ █         █ █                       █
    █████████████████████████ ███████████ █████████████████████████

## Usage

The gallery should have warn you. Games on Chip-8 are not fun.
I mean it's black and white, blinky, no sound. They
all are pale copies of other hits. The only reason you would play a
Chip-8 game is if you are me and you want to wonder at the program you just
wrote and tell you, "It's alive, alive !".

But I can't refrain you of not being me and also wanting to play some Chip-8
games. So here is a quick guide.

### Run on Linux

First of all, you must install [leiningen](http://leiningen.org/).
It's a single shell script, nothing to worry about.

Then clone this repo and jump into it.

Implementation of screen and keyboard is console based so you need to pimp
your console a little before running any game comfortably.

    xset r rate 100; \
    clear; \
    setterm --cursor off; \
    stty -icanon -echo; \
    ~/bin/lein trampoline run qwerty roms/BRIX; \
    xset r rate; \
    setterm  --cursor on; \
    stty sane

Just copy and paste. If you want an explanation, here we are :

- `xset r rate 100` will set a delay of 100 ms for key repeat. This allows
smooth inputs.

- `clear` clear the screen

- `setterm --cursor off` removes the cursor from the term

- `stty -icanon -echo` don't buffer inputs (the program doesn't have to wait
the return key to get user input) and don't echo back user input on screen.

- `lein trampoline run qwerty roms/BRIX;` run arkanoïd clone in a new JVM.
 `qwerty` binds a qwerty keyboard layout. Use `azerty` if it happens
 you have a stupid azerty layout like I do.
  Roms are located under the roms directory.
  You may also specify a URL if you want.

- other commands restores the terminal to factory settings.

### Run on Other Unices

I didn't try. If you try the linux, it may work. I don't care.

### Run on Windows

There is a nice tutorial [here](http://hmpg.net/).


## Contributing

Sharing some code with you would be wonderful ! Hope make me live.

And if you happen to know Clojure, a code review would make my day.
No, a whole year.

## License

Copyright © 2016 Jérôme Prudent

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
