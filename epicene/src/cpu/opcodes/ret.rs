use super::super::{Size, Cycle, Opcode, ComputerUnit};

// TODO use the same structure as ret_cond using JmpCondition::ALWAYS
struct UnconditionalReturn {
    size: Size,
    cycles: Cycle
}

pub fn ret() -> Box<Opcode> {
    Box::new(UnconditionalReturn {
        size: 1,
        cycles: 26
    })
}

impl Opcode for UnconditionalReturn {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let address = cpu.pop();
        cpu.set_register_pc(address - self.size())
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
    fn to_string(&self, _: &ComputerUnit) -> String {
        "ret".to_string()
    }
}
