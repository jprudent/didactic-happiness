use super::super::{Cycle, Word, Double, Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, LeftOperand, Carry, Constant, DoubleRegister, WordRegister, RightOperand, ImmediateWord, RegisterPointer};
use super::super::alu::{ArithmeticResult, ArithmeticLogicalUnit};

struct ArithmeticOperation<X, Y, D: LeftOperand<X> + RightOperand<X> + AsString, S: RightOperand<Y> + AsString> {
    source: S,
    destination: D,
    operation: fn(X, Y, Word) -> ArithmeticResult<X>,
    mnemonic: &'static str,
    size: Size,
    cycles: Cycle
}


pub fn add_sp_w() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: ImmediateWord {},
            destination: DoubleRegister::SP,
            operation: ArithmeticLogicalUnit::add_16_8_signed,
            mnemonic: "add",
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
            mnemonic: "and",
            size: 2,
            cycles: 8,
        })
}

pub fn and_a_a() -> Box<Opcode> {
    and_a(WordRegister::A)
}

pub fn and_a_b() -> Box<Opcode> {
    and_a(WordRegister::B)
}

pub fn and_a_c() -> Box<Opcode> {
    and_a(WordRegister::C)
}

pub fn and_a_d() -> Box<Opcode> {
    and_a(WordRegister::D)
}

pub fn and_a_e() -> Box<Opcode> {
    and_a(WordRegister::E)
}

pub fn and_a_h() -> Box<Opcode> {
    and_a(WordRegister::H)
}

pub fn and_a_l() -> Box<Opcode> {
    and_a(WordRegister::L)
}

fn and_a(r: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: r,
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::and,
            mnemonic: "and",
            size: 1,
            cycles: 4,
        })
}

pub fn and_a_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: RegisterPointer::HL,
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::and,
            mnemonic: "and",
            size: 1,
            cycles: 8,
        })
}
pub fn sub_r(source: WordRegister) -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::sub,
        mnemonic: "sub",
        size: 1,
        cycles: 4,
    })
}

pub fn sub_ptr_r(source: RegisterPointer) -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::sub,
        mnemonic: "sub",
        size: 1,
        cycles: 8,
    })
}

pub fn add_a_r(source: WordRegister) -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::add,
        mnemonic: "add",
        size: 1,
        cycles: 4,
    })
}

pub fn add_a_d8() -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: ImmediateWord {},
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::add,
        mnemonic: "add",
        size: 2,
        cycles: 8,
    })
}

pub fn sub_d8() -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: ImmediateWord {},
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::sub,
        mnemonic: "sub",
        size: 2,
        cycles: 8,
    })
}

pub fn add_ptr_r(source: RegisterPointer) -> Box<Opcode> {
    Box::new(ArithmeticOperation {
        source: source,
        destination: WordRegister::A,
        operation: ArithmeticLogicalUnit::add,
        mnemonic: "add",
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
            mnemonic: "or",
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
    or_r(ArithmeticLogicalUnit::or, WordRegister::L)
}

fn or_r(op: fn(Word, Word, Word) -> ArithmeticResult<Word>, r: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: r,
            destination: WordRegister::A,
            operation: op,
            mnemonic: "or",
            size: 1,
            cycles: 4,
        }
    )
}

pub fn or_a_w() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: ImmediateWord {},
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::or,
            mnemonic: "or",
            size: 2,
            cycles: 8,
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
            operation: ArithmeticLogicalUnit::rotate_right,
            mnemonic: "rrc",
            size: 1,
            cycles: 4,
        }
    )
}

pub fn rrc_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1),
            destination: RegisterPointer::HL,
            operation: ArithmeticLogicalUnit::rotate_right,
            mnemonic: "rrc",
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
            operation: ArithmeticLogicalUnit::rotate_right_through_carry,
            mnemonic: "rr",
            size: 1,
            cycles: 4
        })
}

pub fn rr_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Carry {},
            destination: RegisterPointer::HL,
            operation: ArithmeticLogicalUnit::rotate_right_through_carry,
            mnemonic: "rr",
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
            source: Carry {},
            destination: register,
            operation: ArithmeticLogicalUnit::rotate_left_through_carry,
            mnemonic: "rl",
            size: 1,
            cycles: 4
        })
}

pub fn rl_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Carry {},
            destination: RegisterPointer::HL,
            operation: ArithmeticLogicalUnit::rotate_left_through_carry,
            mnemonic: "rl",
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
            mnemonic: "srl",
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
            mnemonic: "srl",
            size: 1,
            cycles: 12,
        }
    )
}

pub fn adc_a_a() -> Box<Opcode> {
    adc_a_r(WordRegister::A)
}

pub fn adc_a_b() -> Box<Opcode> {
    adc_a_r(WordRegister::B)
}

pub fn adc_a_c() -> Box<Opcode> {
    adc_a_r(WordRegister::C)
}

pub fn adc_a_d() -> Box<Opcode> {
    adc_a_r(WordRegister::D)
}

pub fn adc_a_e() -> Box<Opcode> {
    adc_a_r(WordRegister::E)
}

pub fn adc_a_h() -> Box<Opcode> {
    adc_a_r(WordRegister::H)
}

pub fn adc_a_l() -> Box<Opcode> {
    adc_a_r(WordRegister::L)
}

fn adc_a_r(source: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: source,
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::add_with_carry,
            mnemonic: "adc",
            size: 1,
            cycles: 4,
        }
    )
}

pub fn adc_a_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: RegisterPointer::HL,
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::add_with_carry,
            mnemonic: "adc",
            size: 1,
            cycles: 8,
        }
    )
}

pub fn adc_a_w() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: ImmediateWord {},
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::add_with_carry,
            mnemonic: "adc",
            size: 2,
            cycles: 8,
        }
    )
}

pub fn sbc_a_a() -> Box<Opcode> {
    sbc_a_r(WordRegister::A)
}

pub fn sbc_a_b() -> Box<Opcode> {
    sbc_a_r(WordRegister::B)
}

pub fn sbc_a_c() -> Box<Opcode> {
    sbc_a_r(WordRegister::C)
}

pub fn sbc_a_d() -> Box<Opcode> {
    sbc_a_r(WordRegister::D)
}

pub fn sbc_a_e() -> Box<Opcode> {
    sbc_a_r(WordRegister::E)
}

pub fn sbc_a_h() -> Box<Opcode> {
    sbc_a_r(WordRegister::H)
}

pub fn sbc_a_l() -> Box<Opcode> {
    sbc_a_r(WordRegister::L)
}

fn sbc_a_r(source: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: source,
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::sub_with_carry,
            mnemonic: "sbc",
            size: 1,
            cycles: 4,
        }
    )
}

pub fn sbc_a_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: RegisterPointer::HL,
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::sub_with_carry,
            mnemonic: "sbc",
            size: 1,
            cycles: 8,
        }
    )
}

pub fn sbc_a_w() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: ImmediateWord {},
            destination: WordRegister::A,
            operation: ArithmeticLogicalUnit::sub_with_carry,
            mnemonic: "sbc",
            size: 2,
            cycles: 8,
        }
    )
}

pub fn swap_a() -> Box<Opcode> {
    swap_r(WordRegister::A)
}

pub fn swap_b() -> Box<Opcode> {
    swap_r(WordRegister::B)
}

pub fn swap_c() -> Box<Opcode> {
    swap_r(WordRegister::C)
}

pub fn swap_d() -> Box<Opcode> {
    swap_r(WordRegister::D)
}

pub fn swap_e() -> Box<Opcode> {
    swap_r(WordRegister::E)
}

pub fn swap_h() -> Box<Opcode> {
    swap_r(WordRegister::H)
}

pub fn swap_l() -> Box<Opcode> {
    swap_r(WordRegister::L)
}

fn swap_r(destination: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1), // unused
            destination: destination,
            operation: ArithmeticLogicalUnit::swap,
            mnemonic: "swap",
            size: 1,
            cycles: 4,
        }
    )
}

pub fn swap_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1), // unused
            destination: RegisterPointer::HL,
            operation: ArithmeticLogicalUnit::swap,
            mnemonic: "swap",
            size: 1,
            cycles: 4,
        }
    )
}

pub fn sla_a() -> Box<Opcode> {
    sla_r(WordRegister::A)
}
pub fn sla_b() -> Box<Opcode> {
    sla_r(WordRegister::B)
}
pub fn sla_c() -> Box<Opcode> {
    sla_r(WordRegister::C)
}
pub fn sla_d() -> Box<Opcode> {
    sla_r(WordRegister::D)
}
pub fn sla_e() -> Box<Opcode> {
    sla_r(WordRegister::E)
}
pub fn sla_h() -> Box<Opcode> {
    sla_r(WordRegister::H)
}
pub fn sla_l() -> Box<Opcode> {
    sla_r(WordRegister::L)
}

fn sla_r(r: WordRegister) -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1), // unused
            destination: r,
            operation: ArithmeticLogicalUnit::shift_left,
            mnemonic: "swap",
            size: 1,
            cycles: 8,
        }
    )
}

pub fn sla_ptr_hl() -> Box<Opcode> {
    Box::new(
        ArithmeticOperation {
            source: Constant(1), // unused
            destination: RegisterPointer::HL,
            operation: ArithmeticLogicalUnit::shift_left,
            mnemonic: "swap",
            size: 1,
            cycles: 16,
        }
    )
}


fn bool_to_word(b: bool) -> Word {
    if b { 1 } else { 0 }
}

fn compute<X: Copy, Y, D: LeftOperand<X> + RightOperand<X> + AsString, S: RightOperand<Y> + AsString>(operation: &ArithmeticOperation<X, Y, D, S>, cpu: &mut ComputerUnit) -> ArithmeticResult<X> {
    let a = operation.destination.resolve(cpu);
    let b = operation.source.resolve(cpu);
    let c = bool_to_word(cpu.carry_flag());
    let r = (operation.operation)(a, b, c);
    operation.destination.alter(cpu, r.result());
    r
}

// special implementation where z flag is left untouched
// TODO maybe if FlagRegister values were SET,UNSET,UNTOUCHED we could have a generic implementation
// TODO ... an option could do the trick too!
// TODO if it's not generic ... well it's not generic, should have a proper struct
impl Opcode for ArithmeticOperation<Double, Double, DoubleRegister, DoubleRegister> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let r = compute(self, cpu);
        let z_backup = cpu.zero_flag();
        r.flags().set_flags(cpu);
        //override z flag with backup
        cpu.set_zero_flag(z_backup);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {} {}", self.mnemonic, self.destination.to_string(cpu), self.source.to_string(cpu))
    }
}

impl<X: Copy, D: LeftOperand<X> + RightOperand<X> + AsString, S: RightOperand<Word> + AsString> Opcode for ArithmeticOperation<X, Word, D, S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let r = compute(self, cpu);
        r.flags().set_flags(cpu);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {} {}", self.mnemonic, self.destination.to_string(cpu), self.source.to_string(cpu))
    }
}
