use super::super::{Size, Cycle, Opcode, Double, ComputerUnit};
use super::super::operands::{RightOperand, ImmediateDouble, ConstantAddress};
use super::{JmpCondition};

struct UnconditionalCall<S: RightOperand<Double>> {
    address: S,
    condition: JmpCondition,
    size: Size,
    cycles_when_taken: Cycle,
    cycles_when_not_taken: Cycle
}

pub fn call_a16() -> Box<Opcode> {
    call(JmpCondition::ALWAYS)
}

pub fn call_nc_a16() -> Box<Opcode> {
    call(JmpCondition::NOCARRY)
}

pub fn call_nz_a16() -> Box<Opcode> {
    call(JmpCondition::NONZERO)
}

pub fn call_z_a16() -> Box<Opcode> {
    call(JmpCondition::ZERO)
}

pub fn call_c_a16() -> Box<Opcode> {
    call(JmpCondition::CARRY)
}

fn call(condition: JmpCondition) -> Box<Opcode> {
    Box::new(
        UnconditionalCall {
            address: ImmediateDouble {},
            condition: condition,
            size: 3,
            cycles_when_taken: 24,
            cycles_when_not_taken: 12
        })
}

pub fn rst_38() -> Box<Opcode> {
    Box::new(
        UnconditionalCall {
            address: ConstantAddress(0x38),
            condition: JmpCondition::ALWAYS,
            size: 1,
            cycles_when_taken: 32,
            cycles_when_not_taken: 0,
        }
    )
}

impl<S: RightOperand<Double>> Opcode for UnconditionalCall<S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        if self.condition.matches(cpu) {
            let pc = cpu.get_pc_register();
            cpu.push(pc + self.size());

            let address = self.address.resolve(cpu);
            cpu.set_register_pc(address - self.size()); // self.size() is added afterward
        }
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, cpu: &ComputerUnit) -> Cycle {
        if self.condition.matches(cpu) {
            self.cycles_when_taken
        } else {
            self.cycles_when_not_taken
        }
    }
}
