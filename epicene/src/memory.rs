use super::{Word, Address, Double};
use super::cpu::{set_high_word, set_low_word, low_word, high_word};
use super::program::Program;

use std::cell::RefCell;

pub trait MemoryBacked {
    fn word_at(&self, address: Address) -> Word;
    fn set_word_at(&self, address: Address, word: Word);
}

pub struct MutableWord {
    word: RefCell<Word>
}

// This program is single threaded, so we don't need to worry about concurrent race condition.
// Using this struct kill the benefit of "borrowing"
impl MutableWord {
    pub fn new(init: Word) -> MutableWord {
        MutableWord {
            word: RefCell::new(init)
        }
    }

    pub fn set(&self, new_word: Word) {
        let mut word = self.word.borrow_mut();
        *word = new_word;
    }

    pub fn get(&self) -> Word {
        self.word.borrow().clone()
    }
}

pub struct Ram {
    words: RefCell<Vec<Word>>,
    starting_offset: Address
}

impl Ram {
    pub fn new(size: usize, starting_offset: Address) -> Ram {
        let mut words = vec!();
        words.resize(size, 0);
        Ram {
            starting_offset: starting_offset,
            words: RefCell::new(words),
        }
    }
    fn relative_index(&self, absolute_address: Address) -> usize {
        (absolute_address - self.starting_offset) as usize
    }
}

impl MemoryBacked for Ram {
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
    interrupt_enabled_register: &'a MemoryBacked,
    wram_bank1: Ram,
    wram_bank2: Ram,
    timer: &'a MemoryBacked,
    interrupt_requested_register: &'a MemoryBacked,
    sound: &'a MemoryBacked,
    hram: Ram,
    lcd: &'a MemoryBacked,
    serial: &'a MemoryBacked,
    gbc_prepare_speed_switch: Ram,
    joypad: &'a MemoryBacked,
    video_ram: &'a MemoryBacked
}

impl<'a> Mmu<'a> {
    pub fn new(program: &'a mut Program,
               timer: &'a MemoryBacked,
               interrupt_requested_register: &'a MemoryBacked,
               interrupt_enabled_register: &'a MemoryBacked,
               sound: &'a MemoryBacked,
               lcd: &'a MemoryBacked,
               serial: &'a MemoryBacked,
               joypad: &'a MemoryBacked,
               video_ram: &'a MemoryBacked
    ) -> Mmu<'a> {
        Mmu {
            program: program,
            interrupt_enabled_register: interrupt_enabled_register,
            wram_bank1: Ram::new(0x1000, 0xC000),
            wram_bank2: Ram::new(0x1000, 0xD000),
            timer: timer,
            interrupt_requested_register: interrupt_requested_register,
            sound: sound,
            hram: Ram::new(127, 0xFF80),
            lcd: lcd,
            serial: serial,
            gbc_prepare_speed_switch: Ram::new(1, 0xFF4D),
            joypad: joypad,
            video_ram: video_ram
        }
    }

    pub fn word_at(&self, address: Address) -> Word {
        self.memory_backend(address).word_at(address)
    }

    pub fn double_at(&self, address: Address) -> Double {
        let high = self.word_at(address + 1);
        let low = self.word_at(address);
        set_low_word(set_high_word(0, high), low)
    }

    pub fn set_word_at(&mut self, address: Address, word: Word) {
        self.memory_backend(address).set_word_at(address, word);
    }

    fn memory_backend(&'a self, address: Address) -> &'a MemoryBacked {
        match address {
            address if Mmu::in_range(address, 0x0000, 0x7FFF) => self.program,
            address if Mmu::in_range(address, 0x8000, 0xA000) => self.video_ram,
            address if Mmu::in_range(address, 0xC000, 0xCFFF) => &self.wram_bank1,
            address if Mmu::in_range(address, 0xD000, 0xDFFF) => &self.wram_bank2,
            address if Mmu::in_range(address, 0xFE00, 0xFE9F) => self.video_ram,
            0xFF00 => self.joypad,
            address if Mmu::in_range(address, 0xFF01, 0xFF02) => self.serial,
            address if Mmu::in_range(address, 0xFF05, 0xFF07) => self.timer,
            0xFF0F => self.interrupt_requested_register,
            address if Mmu::in_range(address, 0xFF24, 0xFF26) => self.sound,
            0xFF40 => self.lcd,
            0xFF42 => self.lcd,
            0xFF43 => self.lcd,
            0xFF44 => self.lcd,
            0xFF47 => self.lcd,
            0xFF4D => &self.gbc_prepare_speed_switch,
            0xFF4F => self.lcd,
            0xFF68 => self.lcd,
            0xFF69 => self.lcd,
            address if Mmu::in_range(address, 0xFF80, 0xFFFE) => &self.hram,
            0xFFFF => self.interrupt_enabled_register,
            _ => panic!("not implemented memory backend at {:04X}", address)
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