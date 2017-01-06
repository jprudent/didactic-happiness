use super::super::{Size, Cycle, Opcode, ComputerUnit};

struct Ccf {
    size: Size,
    cycles: Cycle
}

pub fn ccf() -> Box<Opcode> {
    Box::new(Ccf {
        size: 1,
        cycles: 4
    })
}

impl Opcode for Ccf {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let c = cpu.carry_flag();
        cpu.set_carry_flag(!c);
        cpu.set_half_carry_flag(false);
        cpu.set_add_sub_flag(false);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}

