use std::cell::RefCell;
use super::{Word, Address, Device, Cycle};
use super::memory::MemoryBacked;


pub struct Sound {
    nr_52_on_off: RefCell<Word>
}

impl Sound {
    pub fn new() -> Sound {
        Sound {
            nr_52_on_off: RefCell::new(0xF1) // initial value described in GBCPUman.pdf
        }
    }

    fn set_nr_52_on_off(&self, new_word: Word) {
        let is_set = new_word.wrapping_shr(7) == 1;
        self.set_all_sound_interrupt(is_set);
    }

    fn nr_52_on_off(&self) -> Word {
        self.nr_52_on_off.borrow().clone()
    }

    fn set_all_sound_interrupt(&self, status: bool) {
        let mut nr_52 = self.nr_52_on_off.borrow_mut();
        if status {
            *nr_52 = *nr_52 | 0b1000_0000
        } else {
            *nr_52 = *nr_52 & 0b0111_1111
        }
    }
}

impl MemoryBacked for Sound {
    fn word_at(&self, address: Address) -> Word {
        match address {
            0xFF26 => self.nr_52_on_off(),
            _ => panic!("Bad memory mapping for sound at {:04X}", address)
        }
    }

    fn set_word_at(&self, address: Address, word: Word) {
        match address {
            0xFF26 => self.set_nr_52_on_off(word),
            _ => panic!("Bad memory mapping for sound at {:04X}", address)
        }
    }
}

impl Device for Sound {
    fn synchronize(&self, cpu_cycles: Cycle) {
        //TODO FIXME
    }
}

mod test {
    use super::*;
    use std::cell::RefCell;

    #[test]
    fn only_bit_7_is_writable() {
        let sound = Sound {
            nr_52_on_off: RefCell::new(0)
        };

        sound.set_nr_52_on_off(0xFF);
        assert_eq!(sound.nr_52_on_off(), 0x80)
    }
}