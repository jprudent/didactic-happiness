use super::super::super::{Cycle};
use super::super::{Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, LeftOperand, DoubleRegister};


struct Pop {
    destination: DoubleRegister,
    size: Size,
    cycles: Cycle
}

pub fn pop_af() -> Box<Opcode> {
    Box::new(pop_rr(DoubleRegister::AF))
}

pub fn pop_bc() -> Box<Opcode> {
    Box::new(pop_rr(DoubleRegister::BC))
}

pub fn pop_de() -> Box<Opcode> {
    Box::new(pop_rr(DoubleRegister::DE))
}

pub fn pop_hl() -> Box<Opcode> {
    Box::new(pop_rr(DoubleRegister::HL))
}

fn pop_rr(register: DoubleRegister) -> Pop {
    Pop {
        destination: register,
        size: 1,
        cycles: 12
    }
}

impl Opcode for Pop {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let value = cpu.pop();
        self.destination.alter(cpu, value);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {}", "pop", self.destination.to_string(cpu))

    }
}
