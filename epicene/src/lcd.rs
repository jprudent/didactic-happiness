use std::cell::RefCell;
use super::{Word, Address, Device, Cycle};
use super::cpu::ComputerUnit;
use super::memory::MemoryBacked;

pub struct Lcd {
    control: RefCell<Word>,
    y_coordinate: RefCell<Word>,
}

impl Lcd {
    pub fn new() -> Lcd {
        Lcd {
            control: RefCell::new(0x91), // initial value described in GBCPUman.pdf
            y_coordinate: RefCell::new(0),
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
}

impl MemoryBacked for Lcd {
    fn word_at(&self, address: Address) -> Word {
        match address {
            0xFF40 => self.control(),
            0xFF44 => self.y_coordinate(),
            _ => panic!("Bad memory mapping for LCD at {:04X}", address)
        }
    }

    fn set_word_at(&self, address: Address, word: Word) {
        match address {
            0xFF40 => self.set_control(word),
            0xFF44 => self.set_y_coordinate(0),
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