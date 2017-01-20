use super::cpu::ComputerUnit;
use super::{Word, Address};
use super::memory::MemoryBacked;
use std::cell::RefCell;

pub struct Interrupt {
    handler: Address,
    mask: Word,
}


enum InterruptKind {
    VBlank,
    LcdStat,
    Timer,
    Serial,
    Joypad
}

impl Interrupt {
    fn new(interrupt: InterruptKind) -> Interrupt {
        match interrupt {
            InterruptKind::VBlank => Interrupt { handler: 0x40, mask: 0b0001 },
            InterruptKind::LcdStat => Interrupt { handler: 0x48, mask: 0b0010 },
            InterruptKind::Timer => Interrupt { handler: 0x50, mask: 0b0100 },
            InterruptKind::Serial => Interrupt { handler: 0x58, mask: 0b1000 },
            InterruptKind::Joypad => Interrupt { handler: 0x60, mask: 0b10000 },
        }
    }


}

pub struct InterruptRequestRegister {
    register: RefCell<Word>,
    vblank: Interrupt,
    lcd_stat: Interrupt,
    timer: Interrupt,
    serial: Interrupt,
    joypad: Interrupt,
}


impl InterruptRequestRegister {
    pub fn new() -> InterruptRequestRegister {
        InterruptRequestRegister {
            register: RefCell::new(0),
            vblank: Interrupt::new(InterruptKind::VBlank),
            lcd_stat: Interrupt::new(InterruptKind::LcdStat),
            timer: Interrupt::new(InterruptKind::Timer),
            serial: Interrupt::new(InterruptKind::Serial),
            joypad: Interrupt::new(InterruptKind::Joypad),
        }
    }

    pub fn timer(&self) -> bool {
        let register = self.register.borrow();
        *register & self.timer.mask == self.timer.mask
    }

    pub fn request_timer_interrupt(&self) {
        let mut register = self.register.borrow_mut();
        *register = *register | self.timer.mask
    }
}

impl MemoryBacked for InterruptRequestRegister {
    fn word_at(&self, address: Address) -> Word {
        self.register.borrow().clone()
    }

    fn set_word_at(&self, address: Address, word: Word) {
        let mut register = self.register.borrow_mut();
        *register = word
    }
}

pub struct InterruptHandler {}

impl InterruptHandler {
    pub fn process_requested(&self, cpu: &mut ComputerUnit) {
        if cpu.interrupt_master() {
            //for interrupt in self.interrupts.iter() {
            //if interrupt.is_enabled(cpu) && interrupt.is_requested(cpu) {
            //    interrupt.mark_processed(cpu);
            //    let pc = cpu.get_pc_register();
            //    cpu.push(pc);
            //    cpu.disable_interrupt_master();
            //    let handler = interrupt.handler();
            //    cpu.set_register_pc(handler);
            //    // Other interrupts will be processed later
            //    return;
            //}
            //}
        }
    }
}
