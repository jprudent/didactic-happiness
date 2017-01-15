use super::super::{Size, Opcode, ComputerUnit};
use super::super::operands::{WordRegister, RightOperand, LeftOperand};
use super::super::super::{Cycle};

struct Rrca {
    size: Size,
    cycles: Cycle
}

pub fn rrca() -> Box<Opcode> {
    Box::new(Rrca {
        size: 1,
        cycles: 4
    })
}

impl Opcode for Rrca {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let a = WordRegister::A.resolve(cpu);
        let r = a.rotate_right(1);
        WordRegister::A.alter(cpu, r);
        cpu.set_carry_flag((a & 1) != 0);
        cpu.set_zero_flag(false);
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
        "rrca".to_string()
    }
}

