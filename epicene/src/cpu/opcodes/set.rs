use super::super::{Cycle, Word, Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, Constant, WordRegister, RightOperand, LeftOperand, RegisterPointer};

struct Set<S: RightOperand<Word> + LeftOperand<Word>+ AsString> {
    bit: Constant,
    target: S,
    size: Size,
    cycles: Cycle
}

pub fn set_n_r(n: Word, r: WordRegister) -> Box<Opcode> {
    Box::new(Set {
        bit: Constant(n),
        target: r,
        size: 1,
        cycles: 8
    })
}

pub fn set_n_ptr_hl(n: Word) -> Box<Opcode> {
    Box::new(Set {
        bit: Constant(n),
        target: RegisterPointer::HL,
        size: 1,
        cycles: 16
    })
}

impl<S: RightOperand<Word> + LeftOperand<Word> + AsString> Opcode for Set<S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let a = self.target.resolve(cpu);
        let bit = self.bit.resolve(cpu);
        assert!(bit < 8);
        let mask = 1 << bit;
        let r = mask | a;
        self.target.alter(cpu, r)
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("set {}, {}", self.bit.to_string(cpu), self.target.to_string(cpu))
    }
}