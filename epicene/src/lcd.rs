use std::cell::RefCell;
use super::{Word, Address, Device, Cycle};
use super::cpu::ComputerUnit;
use super::memory::MemoryBacked;

struct ColorPalette {
    background_index: RefCell<Word>,
    background_data: RefCell<Word>,
}

impl ColorPalette {
    fn new() -> ColorPalette {
        ColorPalette {
            background_index: RefCell::new(0),
            background_data: RefCell::new(0)
        }
    }

    fn background_index(&self) -> Word {
        self.background_index.borrow().clone()
    }

    fn set_background_index(&self, word: Word) {
        let mut background_index = self.background_index.borrow_mut();
        *background_index = word
    }

    fn background_data(&self) -> Word {
        self.background_data.borrow().clone()
    }

    fn set_background_data(&self, word: Word) {
        let mut background_data = self.background_data.borrow_mut();
        *background_data = word
    }
}

pub struct Lcd {
    control: RefCell<Word>,
    y_coordinate: RefCell<Word>,
    gbc_color_palette: ColorPalette,
    gbc_ram_bank_selector: RefCell<Word>,
}

impl Lcd {
    pub fn new() -> Lcd {
        Lcd {
            control: RefCell::new(0x91), // initial value described in GBCPUman.pdf
            y_coordinate: RefCell::new(0),
            gbc_color_palette: ColorPalette::new(),
            gbc_ram_bank_selector: RefCell::new(0)
        }
    }

    fn set_control(&self, new_word: Word) {
        let mut y = self.control.borrow_mut();
        *y = new_word;
    }

    fn control(&self) -> Word {
        self.control.borrow().clone()
    }

    fn set_y_coordinate(&self, new_word: Word) {
        let mut y = self.y_coordinate.borrow_mut();
        *y = new_word;
    }

    fn y_coordinate(&self) -> Word {
        self.y_coordinate.borrow().clone()
    }

    fn set_gbc_ram_bank_selector(&self, new_word: Word) {
        let mut switch = self.gbc_ram_bank_selector.borrow_mut();
        *switch = new_word;
    }

    fn gbc_ram_bank_selector(&self) -> Word {
        self.gbc_ram_bank_selector.borrow().clone()
    }
}

impl MemoryBacked for Lcd {
    fn word_at(&self, address: Address) -> Word {
        match address {
            0xFF40 => self.control(),
            0xFF44 => self.y_coordinate(),
            0xFF4F => self.gbc_ram_bank_selector(),
            0xFF68 => self.gbc_color_palette.background_index(),
            0xFF69 => self.gbc_color_palette.background_data(),
            _ => panic!("Bad memory mapping for LCD at {:04X}", address)
        }
    }

    fn set_word_at(&self, address: Address, word: Word) {
        match address {
            0xFF40 => self.set_control(word),
            0xFF44 => self.set_y_coordinate(0),
            0xFF4F => self.set_gbc_ram_bank_selector(word),
            0xFF68 => self.gbc_color_palette.set_background_index(word),
            0xFF69 => self.gbc_color_palette.set_background_data(word),
            _ => panic!("Bad memory mapping for LCD at {:04X}", address)
        }
    }
}

impl Device for Lcd {
    fn synchronize(&self, cpu_cycles: Cycle) {
        // TODO FIXME
    }
}

mod test {
    use super::*;
    use super::super::memory::MemoryBacked;

    #[test]
    fn writing_y_coordinate_reset_it() {
        let lcd = Lcd::new();
        lcd.set_y_coordinate(0xFF);
        lcd.set_word_at(0xFF44, 42);
        assert_eq!(lcd.y_coordinate(), 0)
    }
}