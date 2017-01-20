use super::{Word, Address, Double};
use super::cpu::{set_high_word, set_low_word, low_word, high_word};
use super::program::Program;
use super::timer::timer::Timer;
use std::cell::RefCell;

pub trait MemoryBacked {
    fn word_at(&self, address: Address) -> Word;
    fn set_word_at(&self, address: Address, word: Word);
}

struct MemoryRegister {
    word: RefCell<Word>
}

impl MemoryBacked for MemoryRegister {
    fn word_at(&self, _: Address) -> Word {
        let word = self.word.borrow();
        *word
    }

    fn set_word_at(&self, _: Address, new_word: Word) {
        let mut word = self.word.borrow_mut();
        *word = new_word
    }
}

struct Wram {
    words: RefCell<[Word; 0x1000]>,
    starting_offset: Address
}

impl Wram {
    fn relative_index(&self, absolute_address: Address) -> usize {
        (absolute_address - self.starting_offset) as usize
    }
}

impl MemoryBacked for Wram {
    fn word_at(&self, address: Address) -> Word {
        let i = self.relative_index(address);
        let words = self.words.borrow();
        (*words)[i]
    }

    fn set_word_at(&self, address: Address, word: Word) {
        let i = self.relative_index(address);
        let mut words = self.words.borrow_mut();
        (*words)[i] = word
    }
}

pub struct Mmu<'a> {
    program: &'a MemoryBacked,
    interrupt_enabled_register: MemoryRegister,
    wram_bank1: Wram,
    wram_bank2: Wram,
    timer: &'a MemoryBacked
}

impl<'a> Mmu<'a> {
    pub fn new(program: &'a mut Program, timer: &'a MemoryBacked) -> Mmu<'a> {
        Mmu {
            program: program,
            interrupt_enabled_register: MemoryRegister { word: RefCell::new(0) },
            wram_bank1: Wram {
                words: RefCell::new([0; 0x1000]),
                starting_offset: 0xC000
            },
            wram_bank2: Wram {
                words: RefCell::new([0; 0x1000]),
                starting_offset: 0xD000
            },
            timer: timer
        }
    }

    pub fn word_at(&self, address: Address) -> Word {
        if address < 0x8000 {
            self.program.word_at(address)
        } else if address >= 0xC000 && address <= 0xCFFF {
            self.wram_bank1.word_at(address)
        } else if address >= 0xD000 && address <= 0xDFFF {
            self.wram_bank2.word_at(address)
        } else if Mmu::in_range(address, 0xFF05, 0xFF07) {
            self.timer.word_at(address)
        } else if address == 0xFFFF {
            self.interrupt_enabled_register.word_at(address)
        } else {
            panic!("not implemented memory at {:04X}", address)
        }
    }

    pub fn double_at(&self, address: Address) -> Double {
        let high = self.word_at(address + 1);
        let low = self.word_at(address);
        set_low_word(set_high_word(0, high), low)
    }

    pub fn set_word_at(&mut self, address: Address, word: Word) {
        if address < 0x8000 {
            self.program.set_word_at(address, word)
        } else if Mmu::in_range(address, 0xC000, 0xCFFF) {
            self.wram_bank1.set_word_at(address, word)
        } else if Mmu::in_range(address, 0xD000, 0xDFFF) {
            self.wram_bank2.set_word_at(address, word)
        } else if Mmu::in_range(address, 0xFF05, 0xFF07) {
            self.timer.set_word_at(address, word)
        } else if address == 0xFFFF {
            self.interrupt_enabled_register.set_word_at(address, word)
        } else {
            panic!("not implemented memory at {:04X}", address)
        }
    }

    fn in_range(address: Address, low: Address, high: Address) -> bool {
        address >= low && address <= high
    }

    pub fn set_double_at(&mut self, address: Address, double: Double) {
        self.set_word_at(address, low_word(double));
        self.set_word_at(address.wrapping_add(1), high_word(double));
    }
}