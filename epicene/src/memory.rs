use super::{Word, Address, Double};
use super::cpu::{set_high_word, set_low_word, low_word, high_word};
use super::program::Program;


pub struct ArrayBasedMemory {
    words: [Word; 0xFFFF + 1]
}

impl ArrayBasedMemory {
    pub fn new() -> ArrayBasedMemory {
        ArrayBasedMemory {
            words: [0xAA; 0xFFFF + 1]
        }
    }
    pub fn word_at(&self, address: Address) -> Word {
        self.words[address as usize]
    }

    pub fn double_at(&self, address: Address) -> Double {
        let high = self.word_at(address + 1);
        let low = self.word_at(address);
        set_low_word(set_high_word(0, high), low)
    }

    pub fn map(&mut self, p: &Program) {
        for i in 0..p.content.len() {
            self.words[i] = p.content[i];
        }
    }

    pub fn set_word_at(&mut self, address: Address, word: Word) {
        self.words[address as usize] = word;
    }

    pub fn set_double_at(&mut self, address: Address, double: Double) {
        let i = address as usize;
        self.words[i] = low_word(double);
        self.words[i + 1] = high_word(double);
    }
}
