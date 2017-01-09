use super::super::{Size, Word, Cycle, Opcode, ComputerUnit};
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

fn high_nibble(a: Word) -> Word {
    a.wrapping_shr(4)
}

fn low_nibble(a: Word) -> Word {
    a & 0x0F
}

impl Opcode for Daa {
    fn exec(&self, cpu: &mut ComputerUnit) {
        if cpu.add_sub_flag() {
            let a = cpu.get_a_register();
            //--------------------------------------------------------------------------------
            //|           | C Flag  | HEX value in | H Flag | HEX value in | Number  | C flag|
            //| Operation | Before  | upper digit  | Before | lower digit  | added   | After |
            //|           | DAA     | (bit 7-4)    | DAA    | (bit 3-0)    | to byte | DAA   |
            //|------------------------------------------------------------------------------|
            //|   SUB     |    0    |     0-9      |   0    |     0-9      |   00    |   0   |
            //|   SBC     |    0    |     0-8      |   1    |     6-F      |   FA    |   0   |
            //|   DEC     |    1    |     7-F      |   0    |     0-9      |   A0    |   1   |
            //|   NEG     |    1    |     6-F      |   1    |     6-F      |   9A    |   1   |
            if !cpu.carry_flag() && high_nibble(a) <= 9 && !cpu.half_carry_flag() && low_nibble(a) <= 9 {
                cpu.set_carry_flag(false)
            } else if !cpu.carry_flag() && high_nibble(a) <= 8 && cpu.half_carry_flag() && low_nibble(a) >= 6 {
                println!("sub case 2");
                cpu.set_register_a(a.wrapping_add(0xFA));
                cpu.set_carry_flag(false)
            } else if cpu.carry_flag() && high_nibble(a) >= 7 && !cpu.half_carry_flag() && low_nibble(a) <= 9 {
                cpu.set_register_a(a.wrapping_add(0xA0));
                cpu.set_carry_flag(true)
            } else if cpu.carry_flag() && high_nibble(a) >= 6 && cpu.half_carry_flag() && low_nibble(a) >= 6 {
                cpu.set_register_a(a.wrapping_add(0x9A));
                cpu.set_carry_flag(true)
            } else if cpu.carry_flag() && high_nibble(a) >= 0xA && !cpu.half_carry_flag() && low_nibble(a) >= 0xA {
                cpu.set_register_a(a.wrapping_add(0xA0));
                cpu.set_carry_flag(true)
            } else if cpu.carry_flag() && high_nibble(a) <= 0xA && cpu.half_carry_flag() && low_nibble(a) >= 0xA {
                cpu.set_register_a(a.wrapping_add(0x9A));
                cpu.set_carry_flag(true)
            } else {
                let mut new_carry = false;
                if cpu.half_carry_flag() {
                    let r = ArithmeticLogicalUnit::sub(cpu.get_a_register(), 0x06, 0);
                    println!("{:02X} - {:02X} = {:02X} c={}", cpu.get_a_register(), 0x06, r.result(), r.flags().carry_flag());
                    cpu.set_register_a(r.result());
                    cpu.set_carry_flag(r.flags().carry_flag());
                    new_carry = r.flags().carry_flag();
                    println!("new carry = {}", new_carry)
                }
                if cpu.carry_flag() {
                    let r = ArithmeticLogicalUnit::sub(cpu.get_a_register(), 0x60, 0);
                    println!("{:02X} - {:02X} = {:02X} c={}", cpu.get_a_register(), 0x60, r.result(), r.flags().carry_flag());
                    cpu.set_register_a(r.result());
                    cpu.set_carry_flag(r.flags().carry_flag() | new_carry);
                }
            }
        } else {
            let a = cpu.get_a_register();
            println!("a={:02X} ({:1X} {:1X} h={} c={})", a, high_nibble(a), low_nibble(a), cpu.half_carry_flag(), cpu.carry_flag());
            //--------------------------------------------------------------------------------
            //|           | C Flag  | HEX value in | H Flag | HEX value in | Number  | C flag|
            //| Operation | Before  | upper digit  | Before | lower digit  | added   | After |
            //|           | DAA     | (bit 7-4)    | DAA    | (bit 3-0)    | to byte | DAA   |
            //|------------------------------------------------------------------------------|
            //|           |    0    |     0-9      |   0    |     0-9      |   00    |   0   |
            //|   ADD     |    0    |     0-8      |   0    |     A-F      |   06    |   0   |
            //|           |    0    |     0-9      |   1    |     0-3      |   06    |   0   |
            //|   ADC     |    0    |     A-F      |   0    |     0-9      |   60    |   1   |

            //|           |    0    |     9-F      |   0    |     A-F      |   66    |   1   |

            //|   INC     |    0    |     A-F      |   1    |     0-3      |   66    |   1   |

            //|           |    1    |     0-2      |   0    |     0-9      |   60    |   1   |

            //|           |    1    |     0-2      |   0    |     A-F      |   66    |   1   |
            //|           |    1    |     0-3      |   1    |     0-3      |   66    |   1   |
            //|------------------------------------------------------------------------------|
            if !cpu.carry_flag() && high_nibble(a) <= 9 && !cpu.half_carry_flag() && low_nibble(a) <= 9 {
                println!("case 1");
                cpu.set_carry_flag(false)
            } else if !cpu.carry_flag() && high_nibble(a) <= 8 && !cpu.half_carry_flag() && low_nibble(a) >= 0xA {
                println!("case 2");
                cpu.set_carry_flag(false);
                cpu.set_register_a(a.wrapping_add(0x06));
            } else if !cpu.carry_flag() && high_nibble(a) <= 9 && cpu.half_carry_flag() && low_nibble(a) <= 3 {
                println!("case 3");
                cpu.set_carry_flag(false);
                cpu.set_register_a(a.wrapping_add(0x06));
            } else if !cpu.carry_flag() && high_nibble(a) >= 0xA && !cpu.half_carry_flag() && low_nibble(a) <= 9 {
                println!("case 4");
                cpu.set_carry_flag(true);
                cpu.set_register_a(a.wrapping_add(0x60));
            } else if !cpu.carry_flag() && high_nibble(a) >= 0x9 && !cpu.half_carry_flag() && low_nibble(a) >= 0xA {
                println!("case 5");
                cpu.set_carry_flag(true);
                cpu.set_register_a(a.wrapping_add(0x66));
            } else if !cpu.carry_flag() && high_nibble(a) >= 0xA && cpu.half_carry_flag() && low_nibble(a) <= 0x03 {
                println!("case 6");
                cpu.set_carry_flag(true);
                cpu.set_register_a(a.wrapping_add(0x66));
            } else if cpu.carry_flag() && high_nibble(a) <= 0x02 && !cpu.half_carry_flag() && low_nibble(a) <= 0x09 {
                println!("case 7");
                cpu.set_carry_flag(true);
                cpu.set_register_a(a.wrapping_add(0x60));
            } else if cpu.carry_flag() && high_nibble(a) <= 0x02 && !cpu.half_carry_flag() && low_nibble(a) >= 0xA {
                println!("case 8");
                cpu.set_carry_flag(true);
                cpu.set_register_a(a.wrapping_add(0x66));
            } else if cpu.carry_flag() && high_nibble(a) <= 0x03 && cpu.half_carry_flag() && low_nibble(a) <= 0x03 {
                println!("case 9");
                cpu.set_carry_flag(true);
                cpu.set_register_a(a.wrapping_add(0x66));
            } else {
                let mut new_carry = false;
                if cpu.half_carry_flag() || ((cpu.get_a_register() & 0xF) > 9) {
                    let r = ArithmeticLogicalUnit::add(cpu.get_a_register(), 0x06, 0);
                    cpu.set_register_a(r.result());
                    cpu.set_carry_flag(r.flags().carry_flag());
                    new_carry = r.flags().carry_flag()
                }
                if cpu.carry_flag() || cpu.get_a_register() > 0x9F {
                    let r = ArithmeticLogicalUnit::add(cpu.get_a_register(), 0x60, 0);
                    cpu.set_register_a(r.result());
                    cpu.set_carry_flag(r.flags().carry_flag() | new_carry);
                }
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

