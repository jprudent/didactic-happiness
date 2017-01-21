use super::{Word, Address};
use super::memory::{MutableWord, MemoryBacked};

pub struct Joypad {
    status: MutableWord
}

impl Joypad {
    pub fn new() -> Joypad {
        Joypad {
            status: MutableWord::new(0)
        }
    }
}

impl MemoryBacked for Joypad {
    fn word_at(&self, _: Address) -> Word {
        self.status.get()
    }

    fn set_word_at(&self, _: Address, word: Word) {
        self.status.set(word)
    }
}
