use super::super::{Word, Size, Cycle, Opcode, ComputerUnit};
use super::super::operands::{RightOperand, ImmediateWord};
use super::super::alu::{ArithmeticLogicalUnit};

struct CompareA<S: RightOperand<Word>> {
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

impl<S: RightOperand<Word>> Opcode for CompareA<S> {
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
}
