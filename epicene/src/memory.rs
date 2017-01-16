use super::{Word, Address, Double, MemoryInterface};
use super::cpu::{set_high_word, set_low_word, low_word, high_word};
use super::program::Program;
use super::timer::DividerTimer;
use std::collections::HashMap;

pub trait Memory {
    fn word_at(&self, address: Address) -> Word;
    fn double_at(&self, address: Address) -> Double;
    fn map(&mut self, p: &Program);
    fn set_word_at(&mut self, address: Address, word: Word);
    fn set_double_at(&mut self, address: Address, double: Double);
}

pub struct ArrayBasedMemory {
    words: [Word; 0xFFFF + 1]
}

impl ArrayBasedMemory {
    pub fn new() -> ArrayBasedMemory {
        ArrayBasedMemory {
            words: [0xAA; 0xFFFF + 1]
        }
    }
}

impl Memory for ArrayBasedMemory {
    fn word_at(&self, address: Address) -> Word {
        self.words[address as usize]
    }

    fn double_at(&self, address: Address) -> Double {
        let high = self.word_at(address + 1);
        let low = self.word_at(address);
        set_low_word(set_high_word(0, high), low)
    }

    fn map(&mut self, p: &Program) {
        for i in 0..p.content.len() {
            self.words[i] = p.content[i];
        }
    }

    fn set_word_at(&mut self, address: Address, word: Word) {
        self.words[address as usize] = word;
    }

    fn set_double_at(&mut self, address: Address, double: Double) {
        let i = address as usize;
        self.words[i] = low_word(double);
        self.words[i + 1] = high_word(double);
    }
}

pub struct Ram {
    words: [Word; 0xFFFF + 1]
}

impl Ram {
    pub fn new() -> Ram {
        Ram {
            words: [0xAA; 0xFFFF + 1]
        }
    }
}

impl MemoryInterface for Ram {
    fn word_at(&self, address: Address) -> Word {
        self.words[address as usize]
    }

    fn set_word_at(&mut self, address: Address, word: Word) {
        self.words[address as usize] = word;
    }
}

pub struct Mmu<'a> {
    ram: Ram,
    divider_timer: &'a mut MemoryInterface
}

impl<'a> Mmu<'a> {
    pub fn new(divider_timer: &mut DividerTimer) -> Mmu {
        Mmu {
            ram: Ram::new(),
            divider_timer: divider_timer
        }
    }
}

impl<'a> Memory for Mmu<'a> {
    fn word_at(&self, address: Address) -> Word {
        assert!(address <= 0xFFFF);
        if address == 0xFF04 {
            self.divider_timer.word_at(address)
        } else {
            self.ram.word_at(address)
        }
    }

    fn double_at(&self, address: Address) -> Double {
        let high = self.word_at(address + 1);
        let low = self.word_at(address);
        set_low_word(set_high_word(0, high), low)
    }

    fn map(&mut self, p: &Program) {
        unimplemented!()
    }

    fn set_word_at(&mut self, address: Address, word: Word) {
        assert!(address <= 0xFFFF);
        if address == 0xFF04 {
            self.divider_timer.set_word_at(address, word)
        } else {
            self.ram.set_word_at(address, word)
        }
    }

    fn set_double_at(&mut self, address: Address, double: Double) {
        let i = address as usize;
        self.set_word_at(address, low_word(double));
        self.set_word_at(address.wrapping_add(1), high_word(double));
    }
}