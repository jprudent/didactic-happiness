use super::super::{Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, RightOperand, ImmediateDouble, DoubleRegister};
use super::JmpCondition;
use std::marker::PhantomData;
use super::super::super::{Cycle, Double};


struct AbsoluteJump<X, A: RightOperand<X> + AsString> {
    address: A,
    size: Size,
    condition: JmpCondition,
    cycles_when_taken: Cycle,
    cycles_when_not_taken: Cycle,
    operation_type: PhantomData<X>
}

pub fn jp_hl() -> Box<Opcode> {
    Box::new(AbsoluteJump {
        address: DoubleRegister::HL,
        condition: JmpCondition::ALWAYS,
        size: 1,
        cycles_when_taken: 4,
        cycles_when_not_taken: 4,
        operation_type: PhantomData
    })
}

pub fn jp_nn() -> Box<Opcode> {
    jp_cond_nn(JmpCondition::ALWAYS)
}

pub fn jp_nz_nn() -> Box<Opcode> {
    jp_cond_nn(JmpCondition::NONZERO)
}

pub fn jp_nc_nn() -> Box<Opcode> {
    jp_cond_nn(JmpCondition::NOCARRY)
}

pub fn jp_z_nn() -> Box<Opcode> {
    jp_cond_nn(JmpCondition::ZERO)
}

pub fn jp_c_nn() -> Box<Opcode> {
    jp_cond_nn(JmpCondition::CARRY)
}

fn jp_cond_nn(condition: JmpCondition) -> Box<Opcode> {
    Box::new(AbsoluteJump {
        address: ImmediateDouble {},
        condition: condition,
        size: 3,
        cycles_when_taken: 16,
        cycles_when_not_taken: 12,
        operation_type: PhantomData
    })
}

impl<A: RightOperand<Double> + AsString> Opcode for AbsoluteJump<Double, A> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        if self.condition.matches(cpu) {
            let address: Double = self.address.resolve(cpu);
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

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:<4} {} {}", "jp", self.condition.to_string(), self.address.to_string(cpu))
    }
}
