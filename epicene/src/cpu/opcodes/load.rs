use std::marker::PhantomData;
use super::super::{Word, Double, Cycle, Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, WordRegister, DoubleRegister, ImmediateDouble, ImmediateWord, RightOperand, LeftOperand, HighMemoryPointer, ImmediatePointer, RegisterPointer, HlOp, SpRelative};

struct Load<X, L: LeftOperand<X> + AsString, R: RightOperand<X> + AsString> {
    destination: L,
    source: R,
    size: Double,
    cycles: Cycle,
    operation_type: PhantomData<X>
}

pub fn ld_hl_sp_plus_w() -> Box<Opcode> {
    Box::new(
        Load {
            destination: DoubleRegister::HL,
            source: SpRelative {},
            size: 2,
            cycles: 12,
            operation_type: PhantomData
        }
    )
}

pub fn ldh_ptr_a() -> Box<Opcode> {
    Box::new(Load {
        destination: HighMemoryPointer {},
        source: WordRegister::A,
        size: 2,
        cycles: 12,
        operation_type: PhantomData
    })
}

pub fn ldh_a_ptr() -> Box<Opcode> {
    Box::new(
        Load {
            destination: WordRegister::A,
            source: HighMemoryPointer {},
            size: 2,
            cycles: 12,
            operation_type: PhantomData
        })
}

pub fn ld_ptr_r_from_r(destination: RegisterPointer, source: WordRegister) -> Box<Opcode> {
    Box::new(Load {
        destination: destination,
        source: source,
        size: 1,
        cycles: 8,
        operation_type: PhantomData
    })
}

pub fn ld_r_from_w(destination: WordRegister) -> Box<Opcode> {
    Box::new(Load {
        destination: destination,
        source: ImmediateWord {},
        size: 2,
        cycles: 8,
        operation_type: PhantomData
    })
}

pub fn ld_rr_from_ww(destination: DoubleRegister) -> Box<Opcode> {
    Box::new(Load {
        destination: destination,
        source: ImmediateDouble {},
        size: 3,
        cycles: 12,
        operation_type: PhantomData
    })
}

pub fn ld_r_from_ptr_r(destination: WordRegister, source: RegisterPointer) -> Box<Opcode> {
    Box::new(Load {
        destination: destination,
        source: source,
        size: 1,
        cycles: 8,
        operation_type: PhantomData
    })
}

pub fn ld_r_from_r(destination: WordRegister, source: WordRegister) -> Box<Opcode> {
    Box::new(Load {
        destination: destination,
        source: source,
        size: 1,
        cycles: 4,
        operation_type: PhantomData
    })
}

pub fn ld_ptr_nn_from_rr(source: DoubleRegister) -> Box<Opcode> {
    Box::new(Load {
        destination: ImmediatePointer::<Double>::new(),
        source: source,
        size: 3,
        cycles: 20,
        operation_type: PhantomData
    })
}

pub fn ld_ptr_nn_from_r(source: WordRegister) -> Box<Opcode> {
    Box::new(Load {
        destination: ImmediatePointer::<Word>::new(),
        source: source,
        size: 3,
        cycles: 16,
        operation_type: PhantomData
    })
}

pub fn ld_ptr_r_from_w(destination: RegisterPointer) -> Box<Opcode> {
    Box::new(Load {
        destination: destination,
        source: ImmediateWord {},
        size: 2,
        cycles: 12,
        operation_type: PhantomData
    })
}

pub fn ld_rr_from_rr(destination: DoubleRegister, source: DoubleRegister) -> Box<Opcode> {
    Box::new(Load {
        destination: destination,
        source: source,
        size: 1,
        cycles: 8,
        operation_type: PhantomData
    })
}

pub fn ld_r_from_ptr_nn(destination: WordRegister) -> Box<Opcode> {
    Box::new(Load {
        destination: destination,
        source: ImmediatePointer::<Word>::new(),
        size: 3,
        cycles: 16,
        operation_type: PhantomData
    })
}


pub fn ld_ptr_hl_from_a(hlop: HlOp) -> Box<Opcode> {
    Box::new(Load {
        source: WordRegister::A,
        destination: hlop,
        size: 1,
        cycles: 8,
        operation_type: PhantomData
    })
}

pub fn ld_a_from_ptr_hl(hlop: HlOp) -> Box<Opcode> {
    Box::new(Load {
        source: hlop,
        destination: WordRegister::A,
        size: 1,
        cycles: 8,
        operation_type: PhantomData
    })
}


impl<X, L: LeftOperand<X> + AsString, R: RightOperand<X> + AsString> Opcode for Load<X, L, R> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let value = self.source.resolve(cpu);
        self.destination.alter(cpu, value);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {} {}", "ld", self.destination.to_string(cpu), self.source.to_string(cpu))

    }
}
