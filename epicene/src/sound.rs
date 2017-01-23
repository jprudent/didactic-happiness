use super::{Word, Address, Device, Cycle};
use super::memory::{MemoryBacked, MutableWord, Ram};


pub struct Sound {
    nr_50_volume_on_off: MutableWord,
    nr_51_output_term: MutableWord,
    nr_52_on_off: MutableWord,

    nr_10_chan_1_sweep: MutableWord,
    nr_11_chan_1_length: MutableWord,
    nr_12_chan_1_volume: MutableWord,
    nr_13_chan_1_frequency_lo: MutableWord,
    nr_14_chan_1_frequency_hi: MutableWord,

    nr_21_chan_2_length: MutableWord,
    nr_22_chan_2_volume: MutableWord,
    nr_23_chan_2_frequency_lo: MutableWord,
    nr_24_chan_2_frequency_hi: MutableWord,

    nr_30_chan_3_on_off: MutableWord,
    nr_31_chan_3_length: MutableWord,
    nr_32_chan_3_output_level: MutableWord,
    nr_33_chan_3_frequency_lo: MutableWord,
    nr_34_chan_3_frequency_hi: MutableWord,
    nr_35_chan_3_wave_pattern: Ram,

    nr_41_chan_4_length: MutableWord,
    nr_42_chan_4_volume: MutableWord,
    nr_43_chan_4_polynomial_counter: MutableWord,
    nr_44_chan_4_counter: MutableWord,
}

impl Sound {
    pub fn new() -> Sound {
        Sound {
            nr_50_volume_on_off: MutableWord::new(0),
            nr_51_output_term: MutableWord::new(0),
            nr_52_on_off: MutableWord::new(0xF1), // initial value described in GBCPUman.pdf
            nr_10_chan_1_sweep: MutableWord::new(0),
            nr_11_chan_1_length: MutableWord::new(0),
            nr_12_chan_1_volume: MutableWord::new(0),
            nr_13_chan_1_frequency_lo: MutableWord::new(0),
            nr_14_chan_1_frequency_hi: MutableWord::new(0),
            nr_21_chan_2_length: MutableWord::new(0),
            nr_22_chan_2_volume: MutableWord::new(0),
            nr_23_chan_2_frequency_lo: MutableWord::new(0),
            nr_24_chan_2_frequency_hi: MutableWord::new(0),
            nr_30_chan_3_on_off: MutableWord::new(0),
            nr_31_chan_3_length: MutableWord::new(0),
            nr_32_chan_3_output_level: MutableWord::new(0),
            nr_33_chan_3_frequency_lo: MutableWord::new(0),
            nr_34_chan_3_frequency_hi: MutableWord::new(0),
            nr_35_chan_3_wave_pattern: Ram::new(0xF, 0xFF30),
            nr_41_chan_4_length: MutableWord::new(0),
            nr_42_chan_4_volume: MutableWord::new(0),
            nr_43_chan_4_polynomial_counter: MutableWord::new(0),
            nr_44_chan_4_counter: MutableWord::new(0),
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
        self.nr_52_on_off.set(new_val)
    }
}

impl MemoryBacked for Sound {
    fn word_at(&self, address: Address) -> Word {
        match address {
            0xFF10 => self.nr_10_chan_1_sweep.get(),
            0xFF11 => self.nr_11_chan_1_length.get(),
            0xFF12 => self.nr_12_chan_1_volume.get(),
            0xFF13 => self.nr_13_chan_1_frequency_lo.get(),
            0xFF14 => self.nr_14_chan_1_frequency_hi.get(),
            0xFF24 => self.nr_50_volume_on_off.get(),
            0xFF25 => self.nr_51_output_term.get(),
            0xFF26 => self.nr_52_on_off.get(),
            0xFF16 => self.nr_21_chan_2_length.get(),
            0xFF17 => self.nr_22_chan_2_volume.get(),
            0xFF18 => self.nr_23_chan_2_frequency_lo.get(),
            0xFF19 => self.nr_24_chan_2_frequency_hi.get(),
            0xFF20 => self.nr_41_chan_4_length.get(),
            0xFF21 => self.nr_42_chan_4_volume.get(),
            0xFF22 => self.nr_43_chan_4_polynomial_counter.get(),
            0xFF23 => self.nr_44_chan_4_counter.get(),
            0xFF1A => self.nr_30_chan_3_on_off.get(),
            0xFF1B => self.nr_31_chan_3_length.get(),
            0xFF1C => self.nr_32_chan_3_output_level.get(),
            0xFF1D => self.nr_33_chan_3_frequency_lo.get(),
            0xFF1E => self.nr_34_chan_3_frequency_hi.get(),
            address if address >= 0xFF30 && address <= 0xFF3F => self.nr_35_chan_3_wave_pattern.word_at(address),
            _ => panic!("Bad memory mapping for sound at {:04X}", address)
        }
    }

    fn set_word_at(&self, address: Address, word: Word) {
        match address {
            0xFF10 => self.nr_10_chan_1_sweep.set(word),
            0xFF11 => self.nr_11_chan_1_length.set(word),
            0xFF12 => self.nr_12_chan_1_volume.set(word),
            0xFF13 => self.nr_13_chan_1_frequency_lo.set(word),
            0xFF14 => self.nr_14_chan_1_frequency_hi.set(word),
            0xFF24 => self.nr_50_volume_on_off.set(word),
            0xFF25 => self.nr_51_output_term.set(word),
            0xFF26 => self.set_nr_52_on_off(word),
            0xFF16 => self.nr_21_chan_2_length.set(word),
            0xFF17 => self.nr_22_chan_2_volume.set(word),
            0xFF18 => self.nr_23_chan_2_frequency_lo.set(word),
            0xFF19 => self.nr_24_chan_2_frequency_hi.set(word),
            0xFF20 => self.nr_41_chan_4_length.set(word),
            0xFF21 => self.nr_42_chan_4_volume.set(word),
            0xFF22 => self.nr_43_chan_4_polynomial_counter.set(word),
            0xFF23 => self.nr_44_chan_4_counter.set(word),
            0xFF1A => self.nr_30_chan_3_on_off.set(word),
            0xFF1B => self.nr_31_chan_3_length.set(word),
            0xFF1C => self.nr_32_chan_3_output_level.set(word),
            0xFF1D => self.nr_33_chan_3_frequency_lo.set(word),
            0xFF1E => self.nr_34_chan_3_frequency_hi.set(word),
            address if address >= 0xFF30 && address <= 0xFF3F => self.nr_35_chan_3_wave_pattern.set_word_at(address, word),
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