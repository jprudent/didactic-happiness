use super::super::{Size, Cycle, Opcode, Double, ComputerUnit};
use super::super::operands::{RightOperand, ImmediateDouble, ConstantAddress};

struct UnconditionalCall<S: RightOperand<Double>> {
    address: S,
    size: Size,
    cycles: Cycle
}

pub fn call() -> Box<Opcode> {
    Box::new(
        UnconditionalCall {
            address: ImmediateDouble {},
            size: 3,
            cycles: 24
        })
}

pub fn rst_38() -> Box<Opcode> {
    Box::new(
        UnconditionalCall {
            address: ConstantAddress(0x38),
            size: 1,
            cycles: 32
        }
    )
}

impl<S: RightOperand<Double>> Opcode for UnconditionalCall<S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let pc = cpu.get_pc_register();
        cpu.push(pc + self.size());

        let address = self.address.resolve(cpu);
        cpu.set_register_pc(address - self.size()); // self.size() is added afterward
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}
