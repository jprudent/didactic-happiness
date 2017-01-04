use super::super::{Size, Cycle, Opcode, ComputerUnit};

struct EnableInterrupts {
    size: Size,
    cycles: Cycle
}

pub fn ei() -> Box<Opcode> {
    Box::new(EnableInterrupts {
        size: 1,
        cycles: 4
    })
}

impl Opcode for EnableInterrupts {
    fn exec(&self, cpu: &mut ComputerUnit) {
        cpu.enable_interrupt_master()
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}
