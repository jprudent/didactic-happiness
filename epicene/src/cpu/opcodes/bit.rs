use super::super::{Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, Constant, WordRegister, RightOperand, RegisterPointer};
use super::super::super::{Cycle, Word};


struct Bit<S: RightOperand<Word> + AsString> {
    n: Constant,
    source: S,
    size: Size,
    cycles: Cycle
}

pub fn bit_n_r(n: Word, r: WordRegister) -> Box<Opcode> {
    Box::new(Bit {
        n: Constant(n),
        source: r,
        size: 1,
        cycles: 16
    })
}

pub fn bit_n_ptr_hl(n: Word) -> Box<Opcode> {
    Box::new(Bit {
        n: Constant(n),
        source: RegisterPointer::HL,
        size: 1,
        cycles: 8
    })
}

impl<S: RightOperand<Word> + AsString> Opcode for Bit<S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let a = self.source.resolve(cpu);
        let n = self.n.resolve(cpu);
        assert!(n < 8);
        let mask = 1 << n;
        cpu.set_zero_flag((mask & a) == 0);
        cpu.set_add_sub_flag(false);
        cpu.set_half_carry_flag(true);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("bit {}, {}", self.n.to_string(cpu), self.source.to_string(cpu))
    }
}