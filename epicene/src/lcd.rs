use super::{Word, Address, Device, Cycle};
use super::memory::{MemoryBacked, MutableWord};

struct ColorPalette {
    background_index: MutableWord,
    background_data: MutableWord,
}

impl ColorPalette {
    fn new() -> ColorPalette {
        ColorPalette {
            background_index: MutableWord::new(0),
            background_data: MutableWord::new(0)
        }
    }
}

struct MonochromePalette {
    palette_data: MutableWord
}

impl MonochromePalette {
    fn new() -> MonochromePalette {
        MonochromePalette {
            palette_data: MutableWord::new(0),
        }
    }
}

pub struct Lcd {
    control: MutableWord,
    y_coordinate: MutableWord,
    x_scroll: MutableWord,
    y_scroll: MutableWord,
    gbc_color_palette: ColorPalette,
    gb_monochrome_palette: MonochromePalette,
    gbc_ram_bank_selector: MutableWord,
}

impl Lcd {
    pub fn new() -> Lcd {
        Lcd {
            control: MutableWord::new(0x91), // initial value described in GBCPUman.pdf
            y_coordinate: MutableWord::new(0),
            x_scroll: MutableWord::new(0),
            y_scroll: MutableWord::new(0),
            gbc_color_palette: ColorPalette::new(),
            gb_monochrome_palette: MonochromePalette::new(),
            gbc_ram_bank_selector: MutableWord::new(0)
        }
    }

    fn set_control(&self, word: Word) {
        self.control.set(word);
        if self.is_off() {
            self.y_coordinate.set(0)
        }
    }

    fn is_off(&self) -> bool {
        self.control.get() & 0b1000_0000 == 0
    }
}

impl MemoryBacked for Lcd {
    fn word_at(&self, address: Address) -> Word {
        match address {
            0xFF40 => &self.control,
            0xFF42 => &self.y_scroll,
            0xFF43 => &self.x_scroll,
            0xFF44 => &self.y_coordinate,
            0xFF47 => &self.gb_monochrome_palette.palette_data,
            0xFF4F => &self.gbc_ram_bank_selector,
            0xFF68 => &self.gbc_color_palette.background_index,
            0xFF69 => &self.gbc_color_palette.background_data,
            _ => panic!("Bad memory mapping for LCD at {:04X}", address)
        }.get()
    }

    fn set_word_at(&self, address: Address, word: Word) {
        match address {
            0xFF40 => self.set_control(word),
            0xFF42 => self.y_scroll.set(word),
            0xFF43 => self.x_scroll.set(word),
            0xFF44 => self.y_coordinate.set(0),
            0xFF47 => self.gb_monochrome_palette.palette_data.set(word),
            0xFF4F => self.gbc_ram_bank_selector.set(word),
            0xFF68 => self.gbc_color_palette.background_index.set(word),
            0xFF69 => self.gbc_color_palette.background_data.set(word),
            _ => panic!("Bad memory mapping for LCD at {:04X}", address)
        }
    }
}

impl Device for Lcd {
    fn synchronize(&self, _: Cycle) {
        // TODO FIXME
    }
}

mod test {
    use super::Lcd;
    use super::super::memory::MemoryBacked;

    #[test]
    fn writing_y_coordinate_reset_it() {
        let lcd = Lcd::new();
        lcd.y_coordinate.set(0xFF);
        lcd.set_word_at(0xFF44, 42);
        assert_eq!(lcd.y_coordinate.get(), 0)
    }

    #[test]
    fn when_the_lcd_is_off_y_coordinate_is_0() {
        let lcd = Lcd::new();
        lcd.set_word_at(0xFF40, 0b1000_0000); // power on

        lcd.y_coordinate.set(42);
        assert_eq!(lcd.word_at(0xFF44), 42);

        lcd.set_word_at(0xFF40, 0b0000_0000); // power off
        assert_eq!(lcd.word_at(0xFF44), 0);

    }
}