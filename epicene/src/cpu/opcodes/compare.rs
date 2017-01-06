use super::super::{Word, Size, Cycle, Opcode, ComputerUnit};
use super::super::operands::{AsString, RightOperand, ImmediateWord, WordRegister, RegisterPointer};
use super::super::alu::{ArithmeticLogicalUnit};

struct CompareA<S: RightOperand<Word> + AsString> {
    source: S,
    size: Size,
    cycles: Cycle
}

pub fn cp_w() -> Box<Opcode> {
    Box::new(
        CompareA {
            source: ImmediateWord {},
            size: 2,
            cycles: 8
        })
}

pub fn cp_a_a() -> Box<Opcode> {
    cp_a_r(WordRegister::A)
}

pub fn cp_a_b() -> Box<Opcode> {
    cp_a_r(WordRegister::B)
}

pub fn cp_a_c() -> Box<Opcode> {
    cp_a_r(WordRegister::C)
}

pub fn cp_a_d() -> Box<Opcode> {
    cp_a_r(WordRegister::D)
}

pub fn cp_a_e() -> Box<Opcode> {
    cp_a_r(WordRegister::E)
}

pub fn cp_a_h() -> Box<Opcode> {
    cp_a_r(WordRegister::H)
}

pub fn cp_a_l() -> Box<Opcode> {
    cp_a_r(WordRegister::L)
}

fn cp_a_r(r: WordRegister) -> Box<Opcode> {
    Box::new(
        CompareA {
            source: r,
            size: 1,
            cycles: 4
        })
}

pub fn cp_a_ptr_hl() -> Box<Opcode> {
    Box::new(
        CompareA {
            source: RegisterPointer::HL,
            size: 1,
            cycles: 8
        })
}

impl<S: RightOperand<Word> + AsString> Opcode for CompareA<S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let a = cpu.get_a_register();
        let b = self.source.resolve(cpu);
        let r = ArithmeticLogicalUnit::sub(a, b, 0);
        r.flags().set_flags(cpu);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {},{}", "cp", WordRegister::A.to_string(cpu), self.source.to_string(cpu))

    }
}
