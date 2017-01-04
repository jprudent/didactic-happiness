use std::marker::PhantomData;
use super::super::{Word, Cycle, Size, Opcode, ComputerUnit};
use super::super::operands::{WordRegister, RightOperand, LeftOperand, RegisterPointer};
use super::super::alu::{ArithmeticResult, ArithmeticLogicalUnit};
// TODO always Word
struct IncDec<X, D: LeftOperand<X> + RightOperand<X>> {
    destination: D,
    operation: fn(X, X) -> ArithmeticResult<X>,
    size: Size,
    cycles: Cycle,
    operation_type: PhantomData<X> // TODO can be removed ?
}
// todo factorize those 2
pub fn dec_r(destination: WordRegister) -> Box<Opcode> {
    Box::new(IncDec {
        destination: destination,
        operation: ArithmeticLogicalUnit::sub,
        size: 1,
        cycles: 4,
        operation_type: PhantomData
    })
}

pub fn inc_r(destination: WordRegister) -> Box<Opcode> {
    Box::new(IncDec {
        destination: destination,
        operation: ArithmeticLogicalUnit::add,
        size: 1,
        cycles: 4,
        operation_type: PhantomData
    })
}
// todo factorize those 2
pub fn dec_ptr_r(destination: RegisterPointer) -> Box<Opcode> {
    Box::new(IncDec {
        destination: destination,
        operation: ArithmeticLogicalUnit::sub,
        size: 1,
        cycles: 12,
        operation_type: PhantomData
    })
}

pub fn inc_ptr_r(destination: RegisterPointer) -> Box<Opcode> {
    Box::new(IncDec {
        destination: destination,
        operation: ArithmeticLogicalUnit::add,
        size: 1,
        cycles: 12,
        operation_type: PhantomData
    })
}

impl<D: LeftOperand<Word> + RightOperand<Word>> Opcode for IncDec<Word, D> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let x = self.destination.resolve(cpu);
        let r = (self.operation)(x, 1);
        self.destination.alter(cpu, r.result());
        //unfortunately this instruction doesn't set the carry flag
        cpu.set_zero_flag(r.flags().zero_flag());
        cpu.set_add_sub_flag(r.flags().add_sub_flag());
        cpu.set_half_carry_flag(r.flags().half_carry_flag());
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}
