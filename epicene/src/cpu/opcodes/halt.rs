use super::super::{Size, Opcode, ComputerUnit, CpuMode};
use super::super::super::{Cycle};

struct Halt {
    size: Size,
    cycles: Cycle
}

pub fn halt() -> Box<Opcode> {
    Box::new(Halt {
        size: 1,
        cycles: 4
    })
}

impl Opcode for Halt {
    fn exec(&self, cpu: &mut ComputerUnit) {
        if cpu.interrupt_master() {
            cpu.enter(CpuMode::HaltJumpInterruptVector)
        } else {
            let int_e = cpu.word_at(0xFFFF);
            let int_f = cpu.word_at(0xFF0F);
            if int_e & int_f & 0x1F == 0 {
                cpu.enter(CpuMode::HaltContinue)
            } else {
                cpu.enter(CpuMode::HaltBug)
            }
        }
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, _: &ComputerUnit) -> String {
        "halt".to_string()
    }
}

