use super::super::{Size, Opcode, ComputerUnit};
use super::super::super::{Cycle, Word};

struct NotImplemented(Word);

impl Opcode for NotImplemented {
    fn exec(&self, cpu: &mut ComputerUnit) {
        panic!("{:02X} not implemented at {:04X}", self.0, cpu.get_pc_register());
    }
    fn size(&self) -> Size {
        unimplemented!()
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        unimplemented!()
    }
    fn to_string(&self, _: &ComputerUnit) -> String {
        "extermiiiinaaaaate".to_string()
    }
}

pub fn not_implemented(word: Word) -> Box<Opcode> {
    Box::new(NotImplemented(word))
}
