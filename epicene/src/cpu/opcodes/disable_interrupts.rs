use super::super::{Cycle, Size, Opcode, ComputerUnit};

struct DisableInterrupts {
    size: Size,
    cycles: Cycle
}

pub fn di() -> Box<Opcode> {
    Box::new(DisableInterrupts {
        size: 1,
        cycles: 4
    })
}

impl Opcode for DisableInterrupts {
    fn exec(&self, cpu: &mut ComputerUnit) {
        cpu.disable_interrupt_master()
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}
