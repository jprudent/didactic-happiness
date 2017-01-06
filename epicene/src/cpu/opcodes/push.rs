use super::super::{Size, Cycle, Opcode, ComputerUnit};
use super::super::operands::{AsString, RightOperand, DoubleRegister};

struct Push {
    source: DoubleRegister,
    size: Size,
    cycles: Cycle
}

pub fn push_af() -> Box<Opcode> {
    Box::new(push_rr(DoubleRegister::AF))
}

pub fn push_bc() -> Box<Opcode> {
    Box::new(push_rr(DoubleRegister::BC))
}

pub fn push_de() -> Box<Opcode> {
    Box::new(push_rr(DoubleRegister::DE))
}

pub fn push_hl() -> Box<Opcode> {
    Box::new(push_rr(DoubleRegister::HL))
}

fn push_rr(register: DoubleRegister) -> Push {
    Push {
        source: register,
        size: 1,
        cycles: 12
    }
}

impl Opcode for Push {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let value = self.source.resolve(cpu);
        cpu.push(value);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {}", "dec", self.source.to_string(cpu))

    }
}
