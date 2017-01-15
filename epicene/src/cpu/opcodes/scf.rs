use super::super::{Size, Opcode, ComputerUnit};
use super::super::super::{Cycle};

struct Scf {
    size: Size,
    cycles: Cycle
}

pub fn scf() -> Box<Opcode> {
    Box::new(Scf {
        size: 1,
        cycles: 4
    })
}

impl Opcode for Scf {
    fn exec(&self, cpu: &mut ComputerUnit) {
        cpu.set_carry_flag(true);
        cpu.set_half_carry_flag(false);
        cpu.set_add_sub_flag(false);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, _: &ComputerUnit) -> String {
        "scf".to_string()
    }
}

