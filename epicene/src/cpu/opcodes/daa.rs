use super::super::{Word, Size, Cycle, Opcode, ComputerUnit};

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
    // I didn't find any strength to dive into this instruction so
    // I monkey copied the snippet found here : http://forums.nesdev.com/viewtopic.php?t=9088
    // TODO I think this can be refactored using ALU::add / sub
    fn exec(&self, cpu: &mut ComputerUnit) {
        let mut a = cpu.get_a_register() as u16;
        if cpu.add_sub_flag() {
            if cpu.half_carry_flag() {
                a = a.wrapping_sub(6) & 0xFF;
            }
            if cpu.carry_flag() {
                a = a.wrapping_sub(0x60)
            }
        } else {
            if cpu.half_carry_flag() || ((a & 0xF) > 9) {
                a = a + 0x06
            }
            if cpu.carry_flag() || a > 0x9F {
                a += 0x60
            }
        }

        // The original code is :
        //_regs.F &= ~(Flags.H | Flags.Z);
        // I suppose (Flags.H | Flags.Z) = 1010_0000
        // so ~(Flags.H | Flags.Z)       = 0101_1111
        // correponding to the flags     = znhc
        // so I believe this means resetting z and h
        cpu.set_zero_flag(false);
        cpu.set_half_carry_flag(false);

        cpu.set_carry_flag((a & 0x100) != 0);
        a = a & 0xFF;
        cpu.set_zero_flag(a == 0);

        cpu.set_register_a(a as Word);
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

