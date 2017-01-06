use std::marker::PhantomData;
use super::super::{Word, Cycle, Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, WordRegister, RightOperand, LeftOperand, RegisterPointer};
use super::super::alu::{ArithmeticLogicalUnit};

struct Dec<X, D: LeftOperand<X> + RightOperand<X> + AsString> {
    destination: D,
    size: Size,
    cycles: Cycle,
    operation_type: PhantomData<X> // TODO can be removed ?
}

struct Inc<X, D: LeftOperand<X> + RightOperand<X> + AsString> {
    destination: D,
    size: Size,
    cycles: Cycle,
    operation_type: PhantomData<X> // TODO can be removed ?
}

// todo factorize those 2
pub fn dec_r(destination: WordRegister) -> Box<Opcode> {
    Box::new(Dec {
        destination: destination,
        size: 1,
        cycles: 4,
        operation_type: PhantomData
    })
}

pub fn inc_r(destination: WordRegister) -> Box<Opcode> {
    Box::new(Inc {
        destination: destination,
        size: 1,
        cycles: 4,
        operation_type: PhantomData
    })
}
// todo factorize those 2
pub fn dec_ptr_r(destination: RegisterPointer) -> Box<Opcode> {
    Box::new(Dec {
        destination: destination,
        size: 1,
        cycles: 12,
        operation_type: PhantomData
    })
}

pub fn inc_ptr_r(destination: RegisterPointer) -> Box<Opcode> {
    Box::new(Inc {
        destination: destination,
        size: 1,
        cycles: 12,
        operation_type: PhantomData
    })
}

impl<D: LeftOperand<Word> + RightOperand<Word> + AsString> Opcode for Inc<Word, D> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let x = self.destination.resolve(cpu);
        let r = ArithmeticLogicalUnit::inc(x);
        self.destination.alter(cpu, r.result());
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
    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {}", "inc", self.destination.to_string(cpu))
    }
}

impl<D: LeftOperand<Word> + RightOperand<Word> + AsString> Opcode for Dec<Word, D> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let x = self.destination.resolve(cpu);
        let r = ArithmeticLogicalUnit::dec(x);
        self.destination.alter(cpu, r.result());
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
    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {}", "dec", self.destination.to_string(cpu))
    }
}
