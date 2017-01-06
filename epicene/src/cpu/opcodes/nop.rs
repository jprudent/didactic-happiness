use super::super::{Size, Cycle, Opcode, ComputerUnit};

struct Nop {
    size: Size,
    cycles: Cycle
}

pub fn nop() -> Box<Opcode> {
    Box::new(Nop {
        size: 1,
        cycles: 4
    })
}

// stop is implemented as Nop, like in BGB
pub fn stop() -> Box<Opcode> {
    Box::new(Nop {
        size: 2,
        cycles: 8
    })
}

impl Opcode for Nop {
    fn exec(&self, _: &mut ComputerUnit) {}

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
    fn to_string(&self, _: &ComputerUnit) -> String {
       "nop".to_string()
    }
}
