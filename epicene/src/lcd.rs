use super::{Word, Address, Device, Cycle};
use super::memory::{MemoryBacked, MutableWord};
use super::interrupts::InterruptRequestRegister;
use self::gpu::{Gpu, GpuMode};

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
    background_and_window: MutableWord,
    sprite_0: MutableWord,
    sprite_1: MutableWord,

}

impl MonochromePalette {
    fn new() -> MonochromePalette {
        MonochromePalette {
            background_and_window: MutableWord::new(0),
            sprite_0: MutableWord::new(0),
            sprite_1: MutableWord::new(0),
        }
    }
}

//TODO design error: the whole lcd should be used in a refcell, not each member !
//TODO this is true for every device I have implemented so far
pub struct Lcd<'a> {
    control: MutableWord,
    status: MutableWord,
    y_coordinate: MutableWord,
    compare_y_coordinate: MutableWord,
    x_scroll: MutableWord,
    y_scroll: MutableWord,
    window_y: MutableWord,
    window_x: MutableWord,
    gbc_color_palette: ColorPalette,
    gb_monochrome_palette: MonochromePalette,
    gbc_ram_bank_selector: MutableWord,
    interrupt_request_register: &'a InterruptRequestRegister,
    gpu: Gpu
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
            window_y: MutableWord::new(0),
            window_x: MutableWord::new(0),
            gbc_color_palette: ColorPalette::new(),
            gb_monochrome_palette: MonochromePalette::new(),
            gbc_ram_bank_selector: MutableWord::new(0),
            interrupt_request_register: interrupt_request_register,
            gpu: Gpu::new()
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

    fn is_enabled_int_ly_eq_lyc(&self) -> bool {
        self.status.get() & 0b100_0000 != 0
    }

    fn is_enabled_int_oam(&self) -> bool {
        self.status.get() & 0b10_0000 != 0
    }

    fn is_enabled_int_vblank(&self) -> bool {
        self.status.get() & 0b1_0000 != 0
    }

    fn is_enabled_int_hblank(&self) -> bool {
        self.status.get() & 0b1000 != 0
    }

    fn is_some_lcd_int_enabled(&self) -> bool {
        self.is_enabled_int_oam() || self.is_enabled_int_hblank() || self.is_enabled_int_vblank()
    }

    fn set_ly_eq_lyc(&self) {
        self.status.set(self.status.get() | 0b100)
    }

    fn unset_ly_eq_lyc(&self) {
        self.status.set(self.status.get() & !0b100)
    }

    fn set_mode(&self, mode: GpuMode) {
        let status = (self.status.get() & !0b11) | mode as Word;
        self.set_status(status)
    }

    pub fn is_same_mode(&self, mode: GpuMode) -> bool {
        self.status.get() & 0b11 == mode as Word
    }

    pub fn current_line(&self) -> Word {
        self.y_coordinate.get()
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
            0xFF47 => &self.gb_monochrome_palette.background_and_window,
            0xFF48 => &self.gb_monochrome_palette.sprite_0,
            0xFF49 => &self.gb_monochrome_palette.sprite_1,
            0xFF4A => &self.window_y,
            0xFF4B => &self.window_x,
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
            0xFF47 => self.gb_monochrome_palette.background_and_window.set(word),
            0xFF48 => self.gb_monochrome_palette.sprite_0.set(word),
            0xFF49 => self.gb_monochrome_palette.sprite_1.set(word),
            0xFF4A => self.window_y.set(word),
            0xFF4B => self.window_x.set(word),
            0xFF4F => self.gbc_ram_bank_selector.set(word),
            0xFF68 => self.gbc_color_palette.background_index.set(word),
            0xFF69 => self.gbc_color_palette.background_data.set(word),
            _ => panic!("Bad memory mapping for LCD at {:04X}", address)
        }
    }
}

impl<'a> Device for Lcd<'a> {
    fn synchronize(&self, cpu_cycles: Cycle) {
        if self.is_off() {
            return
        }

        self.gpu.synchronize(cpu_cycles);

        self.y_coordinate.set(self.gpu.line());

        if !self.is_same_mode(self.gpu.mode()) && self.is_some_lcd_int_enabled() {
            self.interrupt_request_register.request_lcdstat_interrupt()
        }

        self.set_mode(self.gpu.mode());

        if self.is_ly_eq_lyc() {
            self.set_ly_eq_lyc();
            if self.is_enabled_int_ly_eq_lyc() {
                self.interrupt_request_register.request_lcdstat_interrupt()
            }
        } else {
            self.unset_ly_eq_lyc();
        }
    }
}

mod test {
    use super::Lcd;
    use Device;
    use memory::MemoryBacked;
    use interrupts::{InterruptRequestRegister, Interrupt, InterruptKind};

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

        // When LCD is powered on
        lcd.set_word_at(0xFF40, 0b1000_0000); // power on

        // When LY = 42
        for _ in 0..42 {
            lcd.synchronize(80);
            lcd.synchronize(172);
            lcd.synchronize(204);
        }
        assert_eq!(lcd.word_at(0xFF44), 42);

        // When LYC = 42
        lcd.set_word_at(0xFF45, 42);

        // When LY=LYC check is enabled
        lcd.set_word_at(0xFF41, 0b0100_0000);


        lcd.synchronize(1);


        assert_eq!(lcd.word_at(0xFF41) & 0b1000_0000, 0b1000_0000, "bit 7 is unused and always set");
        assert_eq!(lcd.word_at(0xFF41) & 0b0100_0000, 0b0100_0000, "LY=LYC Check Enable is still set");
        assert_eq!(lcd.word_at(0xFF41) & 0b0000_0100, 0b0000_0100, "LY=LYC");

        let int_lcd_stat = Interrupt::new(InterruptKind::LcdStat);
        assert!(interrupt_request_register.is_requested(&int_lcd_stat), "An LCD stat interrupt should be requested")
    }
}

pub mod gpu {
    use super::super::{Cycle, Word, Device};
    use std::cell::RefCell;

    #[derive(Clone)]
    pub enum GpuMode {
        SearchData = 2,
        Transfert = 3,
        HorizontalBlank = 0,
        VerticalBlank = 1
    }

    pub struct Gpu {
        mode: RefCell<GpuMode>,
        clock: RefCell<Cycle>,
        line: RefCell<Word>
    }

    impl Gpu {
        pub fn new() -> Gpu {
            Gpu {
                mode: RefCell::new(GpuMode::SearchData),
                clock: RefCell::new(0),
                line: RefCell::new(0)
            }
        }

        pub fn mode(&self) -> GpuMode {
            self.mode.borrow().clone()
        }

        fn set_mode(&self, mode: GpuMode) {
            let mut v = self.mode.borrow_mut();
            *v = mode
        }

        fn clock(&self) -> Cycle {
            self.clock.borrow().clone()
        }

        fn set_clock(&self, clock: Cycle) {
            let mut v = self.clock.borrow_mut();
            *v = clock
        }

        pub fn line(&self) -> Word {
            self.line.borrow().clone()
        }

        fn set_line(&self, line: Word) {
            let mut v = self.line.borrow_mut();
            *v = line
        }
    }

    impl Device for Gpu {
        fn synchronize(&self, cpu_cycles: Cycle) {
            self.set_clock(self.clock() + cpu_cycles);
            match self.mode() {
                GpuMode::SearchData => {
                    if self.clock() >= 80 {
                        self.set_mode(GpuMode::Transfert);
                        let new_clock = self.clock() - 80;
                        self.set_clock(new_clock);
                    }
                }
                GpuMode::Transfert => {
                    if self.clock() >= 172 {
                        self.set_mode(GpuMode::HorizontalBlank);
                        let new_clock = self.clock() - 172;
                        self.set_clock(new_clock);
                    }
                }
                GpuMode::HorizontalBlank => {
                    if self.clock() >= 204 {
                        let new_clock = self.clock() - 204;
                        self.set_clock(new_clock);

                        let new_line = self.line() + 1;
                        self.set_line(new_line);

                        if new_line == 143 {
                            // there is no hblank for the last line
                            self.set_mode(GpuMode::VerticalBlank);
                        } else {
                            // next line
                            self.set_mode(GpuMode::SearchData);
                        }
                    }
                }
                GpuMode::VerticalBlank => {
                    // Vertical Blank last exactly 10 lines
                    // 1 line OAM+VRAM+Hblank = 456 cycles
                    if self.clock() >= 456 {
                        let new_clock = self.clock() - 456;
                        self.set_clock(new_clock);

                        let new_line = self.line() + 1;

                        if new_line == 154 {
                            // the 10th line finished, and so the vblank
                            self.set_mode(GpuMode::SearchData);
                            self.set_line(0)
                        } else {
                            self.set_line(new_line);
                        }
                    }
                }
            }
        }
    }
}