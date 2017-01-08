use super::super::{Size, Cycle, Opcode, ComputerUnit};

struct Cpl {
    size: Size,
    cycles: Cycle
}

pub fn cpl() -> Box<Opcode> {
    Box::new(Cpl {
        size: 1,
        cycles: 4
    })
}

impl Opcode for Cpl {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let a = cpu.get_a_register();
        cpu.set_register_a(!a);
        cpu.set_add_sub_flag(true);
        cpu.set_half_carry_flag(true);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }

    fn to_string(&self, _: &ComputerUnit) -> String {
        "cpl".to_string()
    }
}