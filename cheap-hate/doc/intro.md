# Introduction to cheap-hate

1. Create a new project (e15d783)

    lein new cheap-hate
    
2. 
4096 (0x1000) memory locations, all of which are 8 bits (a byte)
so we need 12 bits for addressing

the CHIP-8 interpreter itself occupies the first 512 bytes
it is common to store font data in those lower 512 bytes (0x000-0x200).
most programs written for the original system begin at memory location 512 (0x200)
The uppermost 256 bytes (0xF00-0xFFF) are reserved for display refresh
and the 96 bytes below that (0xEA0-0xEFF) were reserved for call stack, internal use, and other variables.

16 8-bit data registers named from V0 to VF. The VF register doubles as a carry flag.
The address register, which is named I, is 16 bits wide and is used with several opcodes that involve memory operations.

The stack is only used to store return addresses when subroutines are called. The original 1802 version allocated 48 bytes for up to 12 levels of nesting; modern implementations normally have at least 16 levels.

CHIP-8 has two timers. They both count down at 60 hertz, until they reach 0.
Delay timer: This timer is intended to be used for timing the events of games. Its value can be set and read.
Sound timer: This timer is used for sound effects. When its value is nonzero, a beeping sound is made.


http://devernay.free.fr/hacks/chip8/C8TECH10.HTM

http://www.linusakesson.net/programming/tty/


No cycle of dependencies (NS)
