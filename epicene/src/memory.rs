use super::{Word, Address, Double};
use super::cpu::{set_high_word, set_low_word, low_word, high_word};
use super::program::Program;

pub trait MemoryBacked {
    fn word_at(&self, address: Address) -> Word;
    fn set_word_at(&mut self, address: Address, word: Word);
}


pub struct Mmu<'a> {
    program: &'a mut MemoryBacked
}

impl<'a> Mmu<'a> {
    pub fn new(program: &'a mut Program) -> Mmu<'a> {
        Mmu {
            program: program
        }
    }

    pub fn word_at(&self, address: Address) -> Word {
        if address < 0x8000 {
            self.program.word_at(address)
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
        } else {
            panic!("not implemented memory at {:04X}", address)
        }
    }

    pub fn set_double_at(&mut self, address: Address, double: Double) {
        self.set_word_at(address, low_word(double));
        self.set_word_at(address.wrapping_add(1), high_word(double));
    }
}