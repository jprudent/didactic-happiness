use super::super::super::{Address, Cycle, Double};
use super::super::{Size, Opcode, ComputerUnit};
use super::super::operands::{AsString, RightOperand, ImmediateDouble, ConstantAddress};
use super::{JmpCondition};

struct Call<S: RightOperand<Double> + AsString> {
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
        Call {
            address: ImmediateDouble {},
            condition: condition,
            size: 3,
            cycles_when_taken: 24,
            cycles_when_not_taken: 12
        })
}

pub fn rst_00() -> Box<Opcode> {
    rst(0x00)
}

pub fn rst_10() -> Box<Opcode> {
    rst(0x10)
}

pub fn rst_20() -> Box<Opcode> {
    rst(0x20)
}

pub fn rst_30() -> Box<Opcode> {
    rst(0x30)
}

pub fn rst_08() -> Box<Opcode> {
    rst(0x08)
}

pub fn rst_18() -> Box<Opcode> {
    rst(0x18)
}

pub fn rst_28() -> Box<Opcode> {
    rst(0x28)
}

pub fn rst_38() -> Box<Opcode> {
    rst(0x38)
}

fn rst(address: Address) -> Box<Opcode> {
    Box::new(
        Call {
            address: ConstantAddress(address),
            condition: JmpCondition::ALWAYS,
            size: 1,
            cycles_when_taken: 32,
            cycles_when_not_taken: 0,
        }
    )
}

impl<S: RightOperand<Double> + AsString> Opcode for Call<S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        if self.condition.matches(cpu) {
            let pc = cpu.get_pc_register();
            cpu.push(pc + self.size());

            let address = self.address.resolve(cpu);
            cpu.set_register_pc(address.wrapping_sub(self.size())); // self.size() is added afterward
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
        format!("{:<4} {}", "call", self.address.to_string(cpu))
    }
}
