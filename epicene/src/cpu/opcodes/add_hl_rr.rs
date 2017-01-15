use super::super::{Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, DoubleRegister, RightOperand};
use super::super::super::{Cycle, Double};

struct AddHlDoubleRegister {
    source: DoubleRegister,
}

pub fn add_hl_bc() -> Box<Opcode> {
    add_hl_rr(DoubleRegister::BC)
}

pub fn add_hl_de() -> Box<Opcode> {
    add_hl_rr(DoubleRegister::DE)
}

pub fn add_hl_hl() -> Box<Opcode> {
    add_hl_rr(DoubleRegister::HL)
}

pub fn add_hl_sp() -> Box<Opcode> {
    add_hl_rr(DoubleRegister::SP)
}

fn add_hl_rr(rr: DoubleRegister) -> Box<Opcode> {
    Box::new(
        AddHlDoubleRegister {
            source: rr,
        })
}

fn has_carry_16(a: Double, b: Double) -> bool {
    let overflowing_result: u32 = a as u32 + b as u32;
    (overflowing_result & 0x10000) != 0
}

fn has_half_carry_16(a: Double, b: Double) -> bool {
    let ah = a & 0x0FFF;
    let bh = b & 0x0FFF;
    let add = ah + bh;
    (add & 0x1000) != 0
}

impl Opcode for AddHlDoubleRegister {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let hl = cpu.get_hl_register();
        let rr = self.source.resolve(cpu);
        let r = hl.wrapping_add(rr);
        cpu.set_register_hl(r);
        cpu.set_add_sub_flag(false);
        cpu.set_half_carry_flag(has_half_carry_16(hl, rr));
        cpu.set_carry_flag(has_carry_16(hl, rr));
    }

    fn size(&self) -> Size {
        1
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        8
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("add {} {}", DoubleRegister::HL.to_string(cpu), self.source.to_string(cpu))
    }
}
