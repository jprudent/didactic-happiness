use super::{Word, Address, Device, Cycle};
use super::memory::{MemoryBacked, MutableWord};


pub struct Sound {
    nr_50_volume_on_off: MutableWord,
    nr_51_output_term: MutableWord,
    nr_52_on_off: MutableWord,
}

impl Sound {
    pub fn new() -> Sound {
        Sound {
            nr_50_volume_on_off: MutableWord::new(0),
            nr_51_output_term: MutableWord::new(0),
            nr_52_on_off: MutableWord::new(0xF1), // initial value described in GBCPUman.pdf
        }
    }

    fn set_nr_52_on_off(&self, new_word: Word) {
        let is_set = new_word.wrapping_shr(7) == 1;
        self.set_all_sound_interrupt(is_set);
    }

    fn set_all_sound_interrupt(&self, status: bool) {
        let nr_52 = self.nr_52_on_off.get();
        let new_val = if status {
            nr_52 | 0b1000_0000
        } else {
            nr_52 & 0b0111_1111
        };
        println!("new val {}", new_val);
        self.nr_52_on_off.set(new_val)
    }
}

impl MemoryBacked for Sound {
    fn word_at(&self, address: Address) -> Word {
        match address {
            0xFF24 => &self.nr_50_volume_on_off,
            0xFF25 => &self.nr_51_output_term,
            0xFF26 => &self.nr_52_on_off,
            _ => panic!("Bad memory mapping for sound at {:04X}", address)
        }.get()
    }

    fn set_word_at(&self, address: Address, word: Word) {
        match address {
            0xFF24 => self.nr_50_volume_on_off.set(word),
            0xFF25 => self.nr_51_output_term.set(word),
            0xFF26 => self.set_nr_52_on_off(word),
            _ => panic!("Bad memory mapping for sound at {:04X}", address)
        }
    }
}

impl Device for Sound {
    fn synchronize(&self, _: Cycle) {
        //TODO FIXME
    }
}

mod test {
    use super::Sound;
    use super::super::memory::MemoryBacked;

    #[test]
    fn only_bit_7_of_nr_52_is_writable() {
        let sound = Sound::new();
        sound.nr_52_on_off.set(1);
        sound.set_word_at(0xFF26, 0xFF);
        assert_eq!(sound.nr_52_on_off.get(), 0x81)
    }
}