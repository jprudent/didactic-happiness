use super::super::{Cycle, Word, Size, Opcode, ComputerUnit};
use super::super::operands::{LeftOperand, DoubleRegister, WordRegister, RightOperand, ImmediateWord, RegisterPointer};
use super::super::alu::{ArithmeticResult, ArithmeticLogicalUnit};

struct ArithmeticOperationOnRegisterA<X, Y, D: LeftOperand<X> + RightOperand<X>, S: RightOperand<Y>> {
    source: S,
    destination: D,
    operation: fn(X, Y) -> ArithmeticResult<X>,
    size: Size,
    cycles: Cycle
}

pub fn add_sp_w() -> Box<Opcode> {
    Box::new(
        ArithmeticOperationOnRegisterA {
            source: ImmediateWord {},
            destination: DoubleRegister::SP,
            operation: ArithmeticLogicalUnit::add16,
            size: 2,
            cycles: 16
        })
}

pub fn and_w() -> Box<Opcode> {
    Box::new(
        ArithmeticOperationOnRegisterA {
            source: ImmediateWord {},
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::and,
            size: 2,
            cycles: 8,
        })
}

pub fn sub_r(source: WordRegister) -> Box<Opcode> {
    Box::new(ArithmeticOperationOnRegisterA {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::sub,
        size: 1,
        cycles: 4,
    })
}

pub fn sub_ptr_r(source: RegisterPointer) -> Box<Opcode> {
    Box::new(ArithmeticOperationOnRegisterA {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::sub,
        size: 1,
        cycles: 8,
    })
}

pub fn add_a_r(source: WordRegister) -> Box<Opcode> {
    Box::new(ArithmeticOperationOnRegisterA {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::add,
        size: 1,
        cycles: 4,
    })
}

pub fn add_a_d8() -> Box<Opcode> {
    Box::new(ArithmeticOperationOnRegisterA {
        source: ImmediateWord{},
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::add,
        size: 2,
        cycles: 8,
    })
}

pub fn sub_d8() -> Box<Opcode> {
    Box::new(ArithmeticOperationOnRegisterA {
        source: ImmediateWord{},
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::sub,
        size: 2,
        cycles: 8,
    })
}

pub fn add_ptr_r(source: RegisterPointer) -> Box<Opcode> {
    Box::new(ArithmeticOperationOnRegisterA {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::add,
        size: 1,
        cycles: 8,
    })
}

pub fn or_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperationOnRegisterA {
            source: RegisterPointer::HL,
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::or,
            size: 1,
            cycles: 8,
        })
}

pub fn or_a() -> Box<Opcode> {
    or_r(ArithmeticLogicalUnit::or, WordRegister::A)
}

pub fn or_b() -> Box<Opcode> {
    or_r(ArithmeticLogicalUnit::or, WordRegister::B)
}

pub fn or_c() -> Box<Opcode> {
    or_r(ArithmeticLogicalUnit::or, WordRegister::C)
}
pub fn or_d() -> Box<Opcode> {
    or_r(ArithmeticLogicalUnit::or, WordRegister::D)
}
pub fn or_e() -> Box<Opcode> {
    or_r(ArithmeticLogicalUnit::or, WordRegister::E)
}
pub fn or_h() -> Box<Opcode> {
    or_r(ArithmeticLogicalUnit::or, WordRegister::H)
}
pub fn or_l() -> Box<Opcode> {
    or_r(ArithmeticLogicalUnit::or, WordRegister::H)
}

fn or_r(op: fn(Word, Word) -> ArithmeticResult<Word>, r: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperationOnRegisterA {
            source: r,
            destination: WordRegister::A,
            operation: op,
            size: 1,
            cycles: 4,
        }
    )
}



impl<X: Copy, Y, D: LeftOperand<X> + RightOperand<X>, S: RightOperand<Y>> Opcode for ArithmeticOperationOnRegisterA<X, Y, D, S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let b = self.source.resolve(cpu);
        let a = self.destination.resolve(cpu);
        let r = (self.operation)(a, b);
        self.destination.alter(cpu, r.result());
        r.flags().set_flags(cpu);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}
