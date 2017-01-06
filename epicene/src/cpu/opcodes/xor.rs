use super::super::{Word, Cycle, Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, ImmediateWord, WordRegister, RegisterPointer, RightOperand};
//TODO use ArithmeticOperation
struct XorWithA<S: RightOperand<Word> + AsString> {
    source: S,
    size: Size,
    cycles: Cycle
}

pub fn xor_r(r: WordRegister) -> Box<Opcode> {
    Box::new(XorWithA {
        source: r,
        size: 1,
        cycles: 4
    })
}

pub fn xor_ptr_r(r: RegisterPointer) -> Box<Opcode> {
    Box::new(XorWithA {
        source: r,
        size: 2,
        cycles: 8
    })
}

pub fn xor_n() -> Box<Opcode> {
    Box::new(XorWithA {
        source: ImmediateWord {},
        size: 2,
        cycles: 8
    })
}

impl<S: RightOperand<Word> + AsString> Opcode for XorWithA<S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let n = self.source.resolve(cpu);
        let a = cpu.get_a_register();
        let r = a ^ n;
        cpu.set_register_a(r);
        cpu.set_zero_flag(r == 0);
        cpu.set_carry_flag(false);
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
        format!("{:<5} {},{}", "xor", WordRegister::A.to_string(cpu), self.source.to_string(cpu) )
    }
}

