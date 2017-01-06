use super::super::{Cycle, Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, DoubleRegister, RightOperand, LeftOperand};


struct Dec {
    destination: DoubleRegister,
    size: Size,
    cycles: Cycle,
}

pub fn dec_bc() -> Box<Opcode> {
    dec_rr(DoubleRegister::BC)
}

pub fn dec_de() -> Box<Opcode> {
    dec_rr(DoubleRegister::DE)
}

pub fn dec_hl() -> Box<Opcode> {
    dec_rr(DoubleRegister::HL)
}

pub fn dec_sp() -> Box<Opcode> {
    dec_rr(DoubleRegister::SP)
}

fn dec_rr(rr: DoubleRegister) -> Box<Dec> {
    Box::new(
        Dec {
            destination: rr,
            size: 1,
            cycles: 8,
        })
}

impl Opcode for Dec {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let original = self.destination.resolve(cpu);
        let value = original.wrapping_sub(1);
        self.destination.alter(cpu, value);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {}", "dec", self.destination.to_string(cpu))
    }
}

struct Inc {
    destination: DoubleRegister,
    size: Size,
    cycles: Cycle,
}

pub fn inc_hl() -> Box<Opcode> {
    inc_rr(DoubleRegister::HL)
}

pub fn inc_bc() -> Box<Opcode> {
    inc_rr(DoubleRegister::BC)
}

pub fn inc_de() -> Box<Opcode> {
    inc_rr(DoubleRegister::DE)
}

pub fn inc_sp() -> Box<Opcode> {
    inc_rr(DoubleRegister::SP)
}

fn inc_rr(rr: DoubleRegister) -> Box<Opcode> {
    Box::new(
        Inc {
            destination: rr,
            size: 1,
            cycles: 8,
        })
}

impl Opcode for Inc {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let original = self.destination.resolve(cpu);
        let value = original.wrapping_add(1);
        self.destination.alter(cpu, value);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {}", "inc", self.destination.to_string(cpu))
    }
}
