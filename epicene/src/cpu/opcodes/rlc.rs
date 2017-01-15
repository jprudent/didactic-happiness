use super::super::{Size, Opcode, ComputerUnit};
use super::super::operands::{WordRegister, AsString, RightOperand, LeftOperand, RegisterPointer};
use super::super::super::{Cycle, Word};

struct Rlc<T: RightOperand<Word> + LeftOperand<Word> + AsString> {
    target: T,
    size: Size,
    cycles: Cycle
}

pub fn rlc_a() -> Box<Opcode> {
    rlc_r(WordRegister::A)
}

pub fn rlc_b() -> Box<Opcode> {
    rlc_r(WordRegister::B)
}

pub fn rlc_c() -> Box<Opcode> {
    rlc_r(WordRegister::C)
}

pub fn rlc_d() -> Box<Opcode> {
    rlc_r(WordRegister::D)
}

pub fn rlc_e() -> Box<Opcode> {
    rlc_r(WordRegister::E)
}

pub fn rlc_h() -> Box<Opcode> {
    rlc_r(WordRegister::H)
}

pub fn rlc_l() -> Box<Opcode> {
    rlc_r(WordRegister::L)
}

fn rlc_r(r: WordRegister) -> Box<Opcode> {
    Box::new(Rlc {
        target: r,
        size: 1,
        cycles: 4
    })
}

pub fn rlc_ptr_hl() -> Box<Opcode> {
    Box::new(Rlc {
        target: RegisterPointer::HL,
        size: 1,
        cycles: 8
    })
}

impl<T: RightOperand<Word> + LeftOperand<Word> + AsString> Opcode for Rlc<T> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let a = self.target.resolve(cpu);
        let r = a.rotate_left(1);
        self.target.alter(cpu, r);
        cpu.set_carry_flag((a & 0x80) != 0);
        cpu.set_zero_flag(r == 0);
        cpu.set_half_carry_flag(false);
        cpu.set_add_sub_flag(false);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("rlc, {}", self.target.to_string(cpu)).to_string()
    }
}

