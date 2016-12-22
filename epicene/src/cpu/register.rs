#[cfg(test)]
use cpu::Register;
use cpu::ReadableRegisters;
use cpu::Double;
use cpu::Word;

pub struct Registers {
    af: Register<Double>,
    bc: Register<Double>,
    de: Register<Double>,
    hl: Register<Double>,
    sp: Register<Double>,
    pc: Register<Double>
}


impl ReadableRegisters for Registers {
    fn af(&self) -> Register<Double> {
        let r = self.af;
        r
    }

    fn bc(&self) -> Register<Double> {
        self.bc
    }

    fn de(&self) -> Register<Double> {
        self.de
    }

    fn hl(&self) -> Register<Double> {
        self.hl
    }

    fn sp(&self) -> Register<Double> {
        self.sp
    }

    fn pc(&self) -> Register<Double> {
        self.pc
    }

    fn a(&self) -> Register<Word> {
        Register(self.af().value().high_word())
    }

    fn b(&self) -> Register<Word> {
        Register(self.bc().value().high_word())
    }

    fn c(&self) -> Register<Word> {
        Register(self.bc().value().low_word())
    }

    fn d(&self) -> Register<Word> {
        Register(self.de().value().high_word())
    }

    fn e(&self) -> Register<Word> {
        Register(self.de().value().low_word())
    }

    fn h(&self) -> Register<Word> {
        Register(self.hl().value().high_word())
    }
#[cfg(test)]
    fn l(&self) -> Register<Word> {
        Register(self.hl().value().low_word())
    }
}

pub fn new_register() -> Registers {
    Registers {
        af: Register(Double(0x1234)),
        bc: Register(Double(0x1234)),
        de: Register(Double(0x1234)),
        hl: Register(Double(0x1234)),
        sp: Register(Double(0x1234)),
        pc: Register(Double(0x0000))
    }
}

#[test]
fn should_get_value_from_registers() {
    let regs = Registers {
        af: Register(Double(0xAAFF)),
        bc: Register(Double(0xBBCC)),
        de: Register(Double(0xDDEE)),
        hl: Register(Double(0x4411)),
        sp: Register(Double(0x5678)),
        pc: Register(Double(0x8765))
    };
    assert_eq!(regs.af().value(), Double(0xAAFF));
    assert_eq!(regs.a().value(), Word(0xAA));
    assert_eq!(regs.c().value(), Word(0xCC))
}
