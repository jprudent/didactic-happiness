use super::cpu::{CpuMode, ComputerUnit};
use super::{Word, Address};
use super::memory::{MutableWord, MemoryBacked};

#[derive(Debug)]
pub struct Interrupt {
    handler: Address,
    mask: Word,
}

pub enum InterruptKind {
    VBlank,
    LcdStat,
    Timer,
    Serial,
    Joypad
}

impl Interrupt {
    pub fn new(interrupt: InterruptKind) -> Interrupt {
        match interrupt {
            InterruptKind::VBlank => Interrupt { handler: 0x40, mask: 0b0001 },
            InterruptKind::LcdStat => Interrupt { handler: 0x48, mask: 0b0010 },
            InterruptKind::Timer => Interrupt { handler: 0x50, mask: 0b0100 },
            InterruptKind::Serial => Interrupt { handler: 0x58, mask: 0b1000 },
            InterruptKind::Joypad => Interrupt { handler: 0x60, mask: 0b10000 },
        }
    }

    fn is_set(&self, register: Word) -> bool {
        register & self.mask == self.mask
    }

    pub fn unset(&self, register: Word) -> Word {
        register & !self.mask
    }

    pub fn handler(&self) -> Address {
        self.handler
    }
}

pub struct InterruptRequestRegister {
    register: MutableWord,
    //vblank: Interrupt,
    lcd_stat: Interrupt,
    timer: Interrupt,
    //serial: Interrupt,
    //joypad: Interrupt,
}

impl InterruptRequestRegister {
    pub fn new() -> InterruptRequestRegister {
        InterruptRequestRegister {
            register: MutableWord::new(0),
            //vblank: Interrupt::new(InterruptKind::VBlank),
            lcd_stat: Interrupt::new(InterruptKind::LcdStat),
            timer: Interrupt::new(InterruptKind::Timer),
            //serial: Interrupt::new(InterruptKind::Serial),
            //joypad: Interrupt::new(InterruptKind::Joypad),
        }
    }

    pub fn timer(&self) -> bool {
        self.register.get() & self.timer.mask == self.timer.mask
    }

    pub fn request_timer_interrupt(&self) {
        self.register.set(self.register.get() | self.timer.mask)
    }

    pub fn request_lcdstat_interrupt(&self) {
        self.register.set(self.register.get() | self.lcd_stat.mask)
    }

    fn mark_processed(&self, interrupt: &Interrupt) {
        self.register.set(interrupt.unset(self.register.get()))
    }

    pub fn is_requested(&self, interrupt: &Interrupt) -> bool {
        interrupt.is_set(self.register.get())
    }
}

impl MemoryBacked for InterruptRequestRegister {
    fn word_at(&self, _: Address) -> Word {
        self.register.get()
    }

    fn set_word_at(&self, _: Address, word: Word) {
        self.register.set(word)
    }
}

pub struct InterruptEnableRegister {
    register: MutableWord
}

impl InterruptEnableRegister {
    pub fn new() -> InterruptEnableRegister {
        InterruptEnableRegister {
            register: MutableWord::new(0)
        }
    }

    pub fn is_enabled(&self, interrupt: &Interrupt) -> bool {
        interrupt.is_set(self.register.get())
    }
}

impl MemoryBacked for InterruptEnableRegister {
    fn word_at(&self, _: Address) -> Word {
        self.register.get()
    }

    fn set_word_at(&self, _: Address, new_word: Word) {
        self.register.set(new_word)
    }
}

pub struct InterruptHandler<'a> {
    // order matters
    interrupts: Vec<Interrupt>,
    interrupt_enable_register: &'a InterruptEnableRegister,
    interrupt_request_register: &'a InterruptRequestRegister,
}

impl<'a> InterruptHandler<'a> {
    pub fn new(interrupt_enable_register: &'a InterruptEnableRegister,
               interrupt_request_register: &'a InterruptRequestRegister) -> InterruptHandler<'a> {
        InterruptHandler {
            interrupts: vec!(
                Interrupt::new(InterruptKind::VBlank),
                Interrupt::new(InterruptKind::LcdStat),
                Interrupt::new(InterruptKind::Timer),
                Interrupt::new(InterruptKind::Serial),
                Interrupt::new(InterruptKind::Joypad)),
            interrupt_enable_register: interrupt_enable_register,
            interrupt_request_register: interrupt_request_register
        }
    }

    pub fn process_requested(&self, cpu: &mut ComputerUnit) {
        match cpu.mode() {
            &CpuMode::Run => self.process_running_cpu(cpu),
            &CpuMode::HaltJumpInterruptVector => self.process_halted_jump_interrupt_vector(cpu),
            &CpuMode::HaltContinue => self.process_halted_continue(cpu),
            _ => panic!("Unhandled cpu stop mode")
        }
    }

    fn process_halted_continue(&self, cpu: &mut ComputerUnit) {
        if self.find_interrupt().is_some() {
            cpu.enter(CpuMode::Run);
            // cpu will continue it's flow just after the halt instruction
        }
    }

    fn process_halted_jump_interrupt_vector(&self, cpu: &mut ComputerUnit) {
        if let Some(interrupt) = self.find_interrupt() {
            cpu.enter(CpuMode::Run);
            self.jump_interruption_vector(interrupt, cpu)
        }
    }

    fn process_running_cpu(&self, cpu: &mut ComputerUnit) {
        if cpu.interrupt_master() {
            if let Some(interrupt) = self.find_interrupt() {
                self.jump_interruption_vector(interrupt, cpu)
            }
        }
    }

    fn jump_interruption_vector(&self, interrupt: &Interrupt, cpu: &mut ComputerUnit) {
        println!("interrupted {:?}", interrupt);
        self.interrupt_request_register.mark_processed(interrupt);
        let pc = cpu.get_pc_register();
        cpu.push(pc);
        cpu.disable_interrupt_master();
        let handler = interrupt.handler();
        cpu.set_register_pc(handler);
    }

    fn find_interrupt(&self) -> Option<&Interrupt> {
        self.interrupts.iter().find(|&interrupt| self.is_enabled_and_requested(interrupt))
    }

    fn is_enabled_and_requested(&self, interrupt: &Interrupt) -> bool {
        self.interrupt_enable_register.is_enabled(interrupt) &&
            self.interrupt_request_register.is_requested(interrupt)
    }
}
