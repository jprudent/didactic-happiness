use super::super::{Double, Cycle, Size, Opcode, ComputerUnit};
use super::super::operands::{DoubleRegister, RightOperand, LeftOperand};

struct IncDecDouble {
    destination: DoubleRegister,
    operation: fn(Double) -> Double,
    size: Size,
    cycles: Cycle,
}

pub fn dec_bc() -> Box<Opcode> {
    inc_dec_rr(dec, DoubleRegister::BC)
}

pub fn dec_de() -> Box<Opcode> {
    inc_dec_rr(dec, DoubleRegister::DE)
}

pub fn dec_hl() -> Box<Opcode> {
    inc_dec_rr(dec, DoubleRegister::HL)
}

pub fn dec_sp() -> Box<Opcode> {
    inc_dec_rr(dec, DoubleRegister::SP)
}

pub fn inc_hl() -> Box<Opcode> {
    inc_dec_rr(inc, DoubleRegister::HL)
}

pub fn inc_bc() -> Box<Opcode> {
    inc_dec_rr(inc, DoubleRegister::BC)
}

pub fn inc_de() -> Box<Opcode> {
    inc_dec_rr(inc, DoubleRegister::DE)
}

pub fn inc_sp() -> Box<Opcode> {
    inc_dec_rr(inc, DoubleRegister::SP)
}

fn inc(a: Double) -> Double {
    a.wrapping_add(1)
}

fn dec(a: Double) -> Double {
    a.wrapping_sub(1)
}

fn inc_dec_rr(op: fn(Double) -> Double, rr: DoubleRegister) -> Box<IncDecDouble> {
    Box::new(
        IncDecDouble {
            destination: rr,
            operation: op,
            size: 1,
            cycles: 8,
        })
}

impl Opcode for IncDecDouble {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let original = self.destination.resolve(cpu);
        let value = (self.operation)(original);
        self.destination.alter(cpu, value);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}
