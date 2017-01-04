use super::JmpCondition;
use super::super::{Size, Cycle, Opcode, ComputerUnit};

struct ConditionalReturn {
    condition: JmpCondition,
    size: Size,
    cycles_when_taken: Cycle,
    cycles_when_not_taken: Cycle
}

pub fn ret_nc() -> Box<Opcode> {
    Box::new(
        ConditionalReturn {
            condition: JmpCondition::NOCARRY,
            size: 1,
            cycles_when_taken: 20,
            cycles_when_not_taken: 8
        })
}

impl Opcode for ConditionalReturn {
    fn exec(&self, cpu: &mut ComputerUnit) {
        if self.condition.matches(cpu) {
            let address = cpu.pop();
            cpu.set_register_pc(address - self.size())
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
