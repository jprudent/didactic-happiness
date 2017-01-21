use super::{Word, Address, Device, Cycle};
use super::memory::{MemoryBacked, MutableWord};
use super::interrupts::InterruptRequestRegister;

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

pub struct Lcd<'a> {
    control: MutableWord,
    status: MutableWord,
    y_coordinate: MutableWord,
    compare_y_coordinate: MutableWord,
    x_scroll: MutableWord,
    y_scroll: MutableWord,
    gbc_color_palette: ColorPalette,
    gb_monochrome_palette: MonochromePalette,
    gbc_ram_bank_selector: MutableWord,
    interrupt_request_register: &'a InterruptRequestRegister,
}

impl<'a> Lcd<'a> {
    pub fn new(interrupt_request_register: &'a InterruptRequestRegister) -> Lcd<'a> {
        Lcd {
            control: MutableWord::new(0x91), // initial value described in GBCPUman.pdf
            status: MutableWord::new(0),
            y_coordinate: MutableWord::new(0),
            compare_y_coordinate: MutableWord::new(0),
            x_scroll: MutableWord::new(0),
            y_scroll: MutableWord::new(0),
            gbc_color_palette: ColorPalette::new(),
            gb_monochrome_palette: MonochromePalette::new(),
            gbc_ram_bank_selector: MutableWord::new(0),
            interrupt_request_register: interrupt_request_register
        }
    }

    fn set_control(&self, word: Word) {
        self.control.set(word);
        if self.is_off() {
            self.y_coordinate.set(0)
        }
    }

    fn set_status(&self, word: Word) {
        self.status.set(word | 0b1000_0000);
    }

    fn is_off(&self) -> bool {
        self.control.get() & 0b1000_0000 == 0
    }

    fn is_ly_eq_lyc(&self) -> bool {
        self.y_coordinate.get() == self.compare_y_coordinate.get()
    }

    fn check_ly_eq_lyc(&self) -> bool {
        self.status.get() & 0b0100_0000 != 0
    }

    fn set_ly_eq_lyc(&self) {
        self.status.set(self.status.get() | 0b100)
    }

    fn unset_ly_eq_lyc(&self) {
        self.status.set(self.status.get() & !0b100)
    }
}

impl<'a> MemoryBacked for Lcd<'a> {
    fn word_at(&self, address: Address) -> Word {
        match address {
            0xFF40 => &self.control,
            0xFF41 => &self.status,
            0xFF42 => &self.y_scroll,
            0xFF43 => &self.x_scroll,
            0xFF44 => &self.y_coordinate,
            0xFF45 => &self.compare_y_coordinate,
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
            0xFF41 => self.set_status(word),
            0xFF42 => self.y_scroll.set(word),
            0xFF43 => self.x_scroll.set(word),
            0xFF44 => self.y_coordinate.set(0),
            0xFF45 => self.compare_y_coordinate.set(word),
            0xFF47 => self.gb_monochrome_palette.palette_data.set(word),
            0xFF4F => self.gbc_ram_bank_selector.set(word),
            0xFF68 => self.gbc_color_palette.background_index.set(word),
            0xFF69 => self.gbc_color_palette.background_data.set(word),
            _ => panic!("Bad memory mapping for LCD at {:04X}", address)
        }
    }
}

impl<'a> Device for Lcd<'a> {
    fn synchronize(&self, _: Cycle) {
        if self.is_off() {
            return
        }

        if self.is_ly_eq_lyc() {
            self.set_ly_eq_lyc();
            if self.check_ly_eq_lyc() {
                self.interrupt_request_register.request_lcdstat_interrupt()
            }
        } else {
            self.unset_ly_eq_lyc();
        }

    }
}

mod test {
    use super::super::Device;
    use super::Lcd;
    use super::super::memory::MemoryBacked;
    use super::super::interrupts::{InterruptRequestRegister, Interrupt, InterruptKind};

    #[test]
    fn writing_y_coordinate_reset_it() {
        let int_flg = &InterruptRequestRegister::new();
        let lcd = Lcd::new(int_flg);
        lcd.y_coordinate.set(0xFF);
        lcd.set_word_at(0xFF44, 42);
        assert_eq!(lcd.y_coordinate.get(), 0)
    }

    #[test]
    fn when_the_lcd_is_off_y_coordinate_is_0() {
        let int_flg = &InterruptRequestRegister::new();
        let lcd = Lcd::new(int_flg);
        lcd.set_word_at(0xFF40, 0b1000_0000); // power on

        lcd.y_coordinate.set(42);
        assert_eq!(lcd.word_at(0xFF44), 42);

        lcd.set_word_at(0xFF40, 0b0000_0000); // power off
        assert_eq!(lcd.word_at(0xFF44), 0);
    }

    #[test]
    fn only_bits_2_to_6_can_be_written_in_control_register() {}

    #[test]
    fn only_bits_3_to_6_can_be_written_in_status_register() {}

    #[test]
    fn when_ly_eq_lyc_and_check_is_enabled_then_a_stat_int_is_requested() {
        let interrupt_request_register = &InterruptRequestRegister::new();
        let lcd = Lcd::new(interrupt_request_register);

        lcd.set_word_at(0xFF40, 0b1000_0000); // power on

        lcd.y_coordinate.set(42);
        lcd.set_word_at(0xFF45, 42);

        lcd.set_word_at(0xFF41, 0b0100_0000); // enable LY=LYC check

        lcd.synchronize(1);

        assert_eq!(lcd.word_at(0xFF41) & 0b1000_0000, 0b1000_0000, "bit 7 is unused and always set");
        assert_eq!(lcd.word_at(0xFF41) & 0b0100_0000, 0b0100_0000, "LY=LYC Check Enable is still set");
        assert_eq!(lcd.word_at(0xFF41) & 0b0000_0100, 0b0000_0100, "LY=LYC");

        let int_lcd_stat = Interrupt::new(InterruptKind::LcdStat);
        assert!(interrupt_request_register.is_requested(&int_lcd_stat), "An LCD stat interrupt should be requested")
    }
}