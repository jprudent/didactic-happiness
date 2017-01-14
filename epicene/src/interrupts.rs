use super::cpu::ComputerUnit;
use super::{Word, Address};

const INTERRUPT_ENABLED_ADDRESS: Address = 0xFFFF;
const INTERRUPT_REQUEST_ADDRESS: Address = 0xFF0F;

pub struct Interrupt {
    handler: Address,
    mask: Word
}

impl Interrupt {
    fn is_enabled(&self, cpu: &ComputerUnit) -> bool {
        let intrerrupt_enabled = cpu.word_at(INTERRUPT_ENABLED_ADDRESS);
        intrerrupt_enabled & self.mask != 0
    }

    fn is_requested(&self, cpu: &ComputerUnit) -> bool {
        let interrupt_flags = cpu.word_at(INTERRUPT_REQUEST_ADDRESS);
        interrupt_flags & self.mask != 0
    }

    fn mark_processed(&self, cpu: &mut ComputerUnit) {
        let interrupt_flags = cpu.word_at(INTERRUPT_REQUEST_ADDRESS);
        let updated_interrupt_flags = (!self.mask) & interrupt_flags;
        cpu.set_word_at(INTERRUPT_REQUEST_ADDRESS, updated_interrupt_flags)
    }

    fn handler(&self) -> Address {
        self.handler
    }
}

pub struct InterruptHandler {
    interrupts: [Interrupt; 5]
}

const VBLANK: Interrupt = Interrupt { handler: 0x40, mask: 0b0000_0001 };
const LCD_STAT: Interrupt = Interrupt { handler: 0x48, mask: 0b0000_0010 };
const TIMER: Interrupt = Interrupt { handler: 0x50, mask: 0b0000_0100 };
const SERIAL: Interrupt = Interrupt { handler: 0x58, mask: 0b0000_1000 };
const JOYPAD: Interrupt = Interrupt { handler: 0x60, mask: 0b0001_0000 };

pub const INTERRUPTS: InterruptHandler = InterruptHandler {
    interrupts: [VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD]
};

impl InterruptHandler {
    pub fn interrupt(&self, cpu: &mut ComputerUnit) {
        if cpu.interrupt_master() {
            for interrupt in self.interrupts.iter() {
                if interrupt.is_enabled(cpu) && interrupt.is_requested(cpu) {
                    interrupt.mark_processed(cpu);
                    let sp = cpu.get_sp_register();
                    cpu.push(sp);
                    cpu.disable_interrupt_master();
                    let handler = interrupt.handler();
                    cpu.set_register_pc(handler);
                    // Other interrupts will be processed later
                    return;
                }
            }
        }
    }
}
