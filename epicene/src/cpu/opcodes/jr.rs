use std::marker::PhantomData;
use super::JmpCondition;
use super::super::{Word, Cycle, Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, ImmediateWord, RightOperand};

struct ConditionalJump<X, A: RightOperand<X> + AsString> {
    address: A,
    condition: JmpCondition,
    size: Size,
    cycles_when_taken: Cycle,
    cycles_when_not_taken: Cycle,
    operation_type: PhantomData<X>
}

pub fn jr_nz_w() -> Box<Opcode> {
    jr_cond_w(JmpCondition::NONZERO)
}

pub fn jr_nc_w() -> Box<Opcode> {
    jr_cond_w(JmpCondition::NOCARRY)
}

pub fn jr_c_w() -> Box<Opcode> {
    jr_cond_w(JmpCondition::CARRY)
}

pub fn jr_z_w() -> Box<Opcode> {
    jr_cond_w(JmpCondition::ZERO)
}

pub fn jr_w() -> Box<Opcode> {
    jr_cond_w(JmpCondition::ALWAYS)
}

fn jr_cond_w(condition: JmpCondition) -> Box<Opcode> {
    Box::new(
        ConditionalJump {
            address: ImmediateWord {},
            condition: condition,
            size: 2,
            cycles_when_taken: 12,
            cycles_when_not_taken: 8,
            operation_type: PhantomData
        })
}

fn is_negative(word: Word) -> bool {
    word & 0x80 != 0
}

impl<A: RightOperand<Word> + AsString> Opcode for ConditionalJump<Word, A> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let relative_address: Word = self.address.resolve(cpu);
        if self.condition.matches(cpu) {
            //don't use ALU because we don't touch flags
            if is_negative(relative_address) {
                let negative_address = (!relative_address).wrapping_sub(1);
                let address = cpu.get_pc_register().wrapping_sub(negative_address as u16);
                cpu.set_register_pc(address - self.size()); // self.size() is added afterward
            } else {
                let address = cpu.get_pc_register().wrapping_add(relative_address as u16);
                cpu.set_register_pc(address); // self.size() will be added
            };
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
        let relative = self.address.to_string(cpu);
        format!("{:<4} {} {}", "jr", self.condition.to_string(), relative)
    }
}
