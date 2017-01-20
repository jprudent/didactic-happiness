use super::{Device, Address, Cycle, Word};
use super::memory::{MemoryBacked, MutableWord};

pub struct Serial {
    data: MutableWord,
    control: MutableWord,
}

impl Serial {
    pub fn new() -> Serial {
        Serial {
            data: MutableWord::new(0),
            control: MutableWord::new(0)
        }
    }
}

impl MemoryBacked for Serial {
    fn word_at(&self, address: Address) -> Word {
        match address {
            0xFF01 => self.data.get(),
            0xFF02 => self.control.get(),
            _ => panic!("Bad memory mapping for serial at {:04X}", address)
        }
    }

    fn set_word_at(&self, address: Address, word: Word) {
        match address {
            0xFF01 => self.data.set(word),
            0xFF02 => self.control.set(word),
            _ => panic!("Bad memory mapping for serial at {:04X}", address)
        }
    }
}

impl Device for Serial {
    fn synchronize(&self, _: Cycle) {
        // TODO fixme
    }
}

