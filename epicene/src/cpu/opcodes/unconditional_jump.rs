use super::super::{Double, Size, Cycle, Opcode, ComputerUnit};
use super::super::operands::{RightOperand, ImmediateDouble, DoubleRegister};
use std::marker::PhantomData;

struct UnconditionalJump<X, A: RightOperand<X>> {
    address: A,
    size: Size,
    cycles: Cycle,
    operation_type: PhantomData<X>
}

pub fn jp_hl() -> Box<Opcode> {
    Box::new(UnconditionalJump {
        address: DoubleRegister::HL,
        size: 1,
        cycles: 4,
        operation_type: PhantomData
    })
}

pub fn jp_nn() -> Box<Opcode> {
    Box::new(UnconditionalJump {
        address: ImmediateDouble {},
        size: 3,
        cycles: 16,
        operation_type: PhantomData
    })
}

impl<A: RightOperand<Double>> Opcode for UnconditionalJump<Double, A> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let address: Double = self.address.resolve(cpu);
        cpu.set_register_pc(address - self.size()); // self.size() is added afterward
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}
