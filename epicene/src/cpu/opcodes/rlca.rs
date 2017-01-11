use super::super::{Size, Cycle, Opcode, ComputerUnit};
use super::super::operands::{WordRegister, RightOperand, LeftOperand};

struct Rlca {
    size: Size,
    cycles: Cycle
}

pub fn rlca() -> Box<Opcode> {
    Box::new(Rlca {
        size: 1,
        cycles: 4
    })
}

impl Opcode for Rlca {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let a = WordRegister::A.resolve(cpu);
        let r = a.rotate_left(1);
        WordRegister::A.alter(cpu, r);
        cpu.set_carry_flag((a & 0x80) != 0);
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
        "rlca".to_string()
    }
}

