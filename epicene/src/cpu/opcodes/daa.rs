use super::super::{ Size, Cycle, Opcode, ComputerUnit};
use super::super::alu::ArithmeticLogicalUnit;

struct Daa {
    size: Size,
    cycles: Cycle
}

pub fn daa() -> Box<Opcode> {
    Box::new(Daa {
        size: 1,
        cycles: 4
    })
}

impl Opcode for Daa {
    fn exec(&self, cpu: &mut ComputerUnit) {
        if cpu.add_sub_flag() {
            let mut nibble_carry = false;
            if cpu.half_carry_flag()  {
                let r = ArithmeticLogicalUnit::sub(cpu.get_a_register(), 0x06, 0);
                //println!("{:02X} - {:02X} = {:02X} c={}", cpu.get_a_register(), 0x06, r.result(), r.flags().carry_flag());
                cpu.set_register_a(r.result());
                nibble_carry = r.flags().carry_flag();
                //cpu.set_carry_flag(r.flags().carry_flag());
            }
            if  nibble_carry || cpu.carry_flag()  {
                let r = ArithmeticLogicalUnit::sub(cpu.get_a_register(), 0x60, 0);
                //println!("{:02X} - {:02X} = {:02X} c={}", cpu.get_a_register(), 0x60, r.result(), r.flags().carry_flag());
                cpu.set_register_a(r.result());
                cpu.set_carry_flag(true);
            }
        } else {
            let mut nibble_carry = false;
            if  cpu.half_carry_flag() || ((cpu.get_a_register() & 0xF) > 9) {
                let r = ArithmeticLogicalUnit::add(cpu.get_a_register(), 0x06, 0);
                cpu.set_register_a(r.result());
                nibble_carry = r.flags().carry_flag()
            }
            if nibble_carry || cpu.carry_flag() || cpu.get_a_register() > 0x9F {
                let r = ArithmeticLogicalUnit::add(cpu.get_a_register(), 0x60, 0);
                cpu.set_register_a(r.result());
                cpu.set_carry_flag(true);
            }
        };

        let a = cpu.get_a_register();
        cpu.set_zero_flag(a == 0);
        cpu.set_half_carry_flag(false);

    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
    fn to_string(&self, _: &ComputerUnit) -> String {
        "daa".to_string()
    }
}

