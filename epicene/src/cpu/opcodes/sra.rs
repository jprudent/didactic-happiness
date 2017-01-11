use super::super::{Size, Word, Cycle, Opcode, ComputerUnit};
use super::super::operands::{WordRegister, AsString, RightOperand, LeftOperand, RegisterPointer};

struct Sra<T: RightOperand<Word> + LeftOperand<Word> + AsString> {
    target: T,
    size: Size,
    cycles: Cycle
}

pub fn sra_a() -> Box<Opcode> {
    sra_r(WordRegister::A)
}

pub fn sra_b() -> Box<Opcode> {
    sra_r(WordRegister::B)
}

pub fn sra_c() -> Box<Opcode> {
    sra_r(WordRegister::C)
}

pub fn sra_d() -> Box<Opcode> {
    sra_r(WordRegister::D)
}

pub fn sra_e() -> Box<Opcode> {
    sra_r(WordRegister::E)
}

pub fn sra_h() -> Box<Opcode> {
    sra_r(WordRegister::H)
}

pub fn sra_l() -> Box<Opcode> {
    sra_r(WordRegister::L)
}

fn sra_r(r: WordRegister) -> Box<Opcode> {
    Box::new(Sra {
        target: r,
        size: 1,
        cycles: 4
    })
}

pub fn sra_ptr_hl() -> Box<Opcode> {
    Box::new(Sra {
        target: RegisterPointer::HL,
        size: 1,
        cycles: 8
    })
}

impl<T: RightOperand<Word> + LeftOperand<Word> + AsString> Opcode for Sra<T> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let a = self.target.resolve(cpu);
        let r = a.wrapping_shr(1) | (a & 0x80);
        self.target.alter(cpu, r);
        cpu.set_carry_flag((a & 1) != 0);
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
        format!("sra, {}", self.target.to_string(cpu)).to_string()
    }
}

