use super::super::{Cycle, Word, Size, Opcode, ComputerUnit};
use super::super::operands::{LeftOperand, Carry, Constant, DoubleRegister, WordRegister, RightOperand, ImmediateWord, RegisterPointer};
use super::super::alu::{ArithmeticResult, ArithmeticLogicalUnit};

struct ArithmeticOperation<X, Y, D: LeftOperand<X> + RightOperand<X>, S: RightOperand<Y>> {
    source: S,
    destination: D,
    operation: fn(X, Y) -> ArithmeticResult<X>,
    size: Size,
    cycles: Cycle
}

pub fn add_sp_w() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: ImmediateWord {},
            destination: DoubleRegister::SP,
            operation: ArithmeticLogicalUnit::add16,
            size: 2,
            cycles: 16
        })
}

pub fn and_w() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: ImmediateWord {},
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::and,
            size: 2,
            cycles: 8,
        })
}

pub fn sub_r(source: WordRegister) -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::sub,
        size: 1,
        cycles: 4,
    })
}

pub fn sub_ptr_r(source: RegisterPointer) -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::sub,
        size: 1,
        cycles: 8,
    })
}

pub fn add_a_r(source: WordRegister) -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::add,
        size: 1,
        cycles: 4,
    })
}

pub fn add_a_d8() -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: ImmediateWord {},
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::add,
        size: 2,
        cycles: 8,
    })
}

pub fn sub_d8() -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: ImmediateWord {},
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::sub,
        size: 2,
        cycles: 8,
    })
}

pub fn add_ptr_r(source: RegisterPointer) -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::add,
        size: 1,
        cycles: 8,
    })
}

pub fn or_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
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
        ArithmeticOperation {
            source: r,
            destination: WordRegister::A,
            operation: op,
            size: 1,
            cycles: 4,
        }
    )
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

fn rlc_r(sd: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1),
            destination: sd,
            operation: ArithmeticLogicalUnit::rlc,
            size: 1,
            cycles: 4,
        }
    )
}

pub fn rlc_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1),
            destination: RegisterPointer::HL,
            operation: ArithmeticLogicalUnit::rlc,
            size: 1,
            cycles: 12,
        }
    )
}

pub fn rrc_a() -> Box<Opcode> {
    rrc_r(WordRegister::A)
}

pub fn rrc_b() -> Box<Opcode> {
    rrc_r(WordRegister::B)
}

pub fn rrc_c() -> Box<Opcode> {
    rrc_r(WordRegister::C)
}

pub fn rrc_d() -> Box<Opcode> {
    rrc_r(WordRegister::D)
}

pub fn rrc_e() -> Box<Opcode> {
    rrc_r(WordRegister::E)
}

pub fn rrc_h() -> Box<Opcode> {
    rrc_r(WordRegister::H)
}

pub fn rrc_l() -> Box<Opcode> {
    rrc_r(WordRegister::L)
}

fn rrc_r(r: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1),
            destination: r,
            operation: ArithmeticLogicalUnit::rrc,
            size:1,
            cycles: 4,
        }
    )
}

pub fn rrc_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1),
            destination: RegisterPointer::HL,
            operation: ArithmeticLogicalUnit::rrc,
            size: 1,
            cycles: 12,
        }
    )
}

pub fn rr_a() -> Box<Opcode> {
    rr_r(WordRegister::A)
}

pub fn rr_b() -> Box<Opcode> {
    rr_r(WordRegister::B)
}

pub fn rr_c() -> Box<Opcode> {
    rr_r(WordRegister::C)
}

pub fn rr_d() -> Box<Opcode> {
    rr_r(WordRegister::D)
}

pub fn rr_e() -> Box<Opcode> {
    rr_r(WordRegister::E)
}

pub fn rr_h() -> Box<Opcode> {
    rr_r(WordRegister::H)
}

pub fn rr_l() -> Box<Opcode> {
    rr_r(WordRegister::L)
}

fn rr_r(register: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Carry {},
            destination: register,
            operation: ArithmeticLogicalUnit::rr,
            size: 1,
            cycles: 4
        })
}

pub fn rr_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Carry {},
            destination: RegisterPointer::HL,
            operation: ArithmeticLogicalUnit::rr,
            size: 1,
            cycles: 12
        })
}

pub fn rl_a() -> Box<Opcode> {
    rl_r(WordRegister::A)
}

pub fn rl_b() -> Box<Opcode> {
    rl_r(WordRegister::B)
}

pub fn rl_c() -> Box<Opcode> {
    rl_r(WordRegister::C)
}

pub fn rl_d() -> Box<Opcode> {
    rl_r(WordRegister::D)
}

pub fn rl_e() -> Box<Opcode> {
    rl_r(WordRegister::E)
}

pub fn rl_h() -> Box<Opcode> {
    rl_r(WordRegister::H)
}

pub fn rl_l() -> Box<Opcode> {
    rl_r(WordRegister::L)
}

fn rl_r(register: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Carry{},
            destination: register,
            operation: ArithmeticLogicalUnit::rl,
            size: 1,
            cycles: 4
        })
}

pub fn rl_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Carry {},
            destination: RegisterPointer::HL,
            operation: ArithmeticLogicalUnit::rl,
            size: 1,
            cycles: 12
        })
}

pub fn srl_a() -> Box<Opcode> {
    srl_r(WordRegister::A)
}

pub fn srl_b() -> Box<Opcode> {
    srl_r(WordRegister::B)
}

pub fn srl_c() -> Box<Opcode> {
    srl_r(WordRegister::C)
}

pub fn srl_d() -> Box<Opcode> {
    srl_r(WordRegister::D)
}

pub fn srl_e() -> Box<Opcode> {
    srl_r(WordRegister::E)
}

pub fn srl_h() -> Box<Opcode> {
    srl_r(WordRegister::H)
}

pub fn srl_l() -> Box<Opcode> {
    srl_r(WordRegister::L)
}

fn srl_r(sd: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1),
            destination: sd,
            operation: ArithmeticLogicalUnit::shift_right,
            size: 1,
            cycles: 4,
        }
    )
}

pub fn srl_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1),
            destination: RegisterPointer::HL,
            operation: ArithmeticLogicalUnit::shift_right,
            size: 1,
            cycles: 12,
        }
    )
}


impl<X: Copy, Y, D: LeftOperand<X> + RightOperand<X>, S: RightOperand<Y>> Opcode for ArithmeticOperation<X, Y, D, S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let a = self.destination.resolve(cpu);
        let b = self.source.resolve(cpu);
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
