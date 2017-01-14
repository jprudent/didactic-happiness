use super::super::{Size, Cycle, Opcode, ComputerUnit};

// TODO use the same structure as ret_cond using JmpCondition::ALWAYS
struct UnconditionalReturn {
    size: Size,
    cycles: Cycle
}

pub fn ret() -> Box<Opcode> {
    Box::new(UnconditionalReturn {
        size: 1,
        cycles: 16
    })
}

impl Opcode for UnconditionalReturn {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let address = cpu.pop();
        println!("ret {:04X}", address);
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


struct UnconditionalReturnInterrupt {
    size: Size,
    cycles: Cycle
}

pub fn reti() -> Box<Opcode> {
    Box::new(UnconditionalReturnInterrupt {
        size: 1,
        cycles: 16
    })
}

impl Opcode for UnconditionalReturnInterrupt {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let address = cpu.pop();
        cpu.set_register_pc(address - self.size());
        cpu.enable_interrupt_master()
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
