#[cfg(test)]

#[derive(Clone,Copy)]
struct Register<V: RegisterValue>(V);

impl<V: RegisterValue + Copy> Register<V> {
    fn value(&self) -> V {
        self.0
    }
}

trait RegisterValue {}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
struct Word(u8);

impl RegisterValue for Word {

}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
struct Double(u16);

impl Double {
    fn high_word(&self) -> Word {
        Word(self.0.wrapping_shr(8) as u8)
    }

    fn low_word(&self) -> Word {
        Word((self.0 & 0xFF) as u8)
    }
}

impl RegisterValue for Double {

}

trait Registers {
    fn af(&self) -> Register<Double>;
    fn bc(&self) -> Register<Double>;
    fn de(&self) -> Register<Double>;
    fn hl(&self) -> Register<Double>;
    fn sp(&self) -> Register<Double>;
    fn pc(&self) -> Register<Double>;
    fn a(&self) -> Register<Word>;
    fn b(&self) -> Register<Word>;
    fn c(&self) -> Register<Word>;
    fn d(&self) -> Register<Word>;
    fn e(&self) -> Register<Word>;
    fn h(&self) -> Register<Word>;
    fn l(&self) -> Register<Word>;
}

struct StructRegisters {
    af: Register<Double>,
    bc: Register<Double>,
    de: Register<Double>,
    hl: Register<Double>,
    sp: Register<Double>,
    pc: Register<Double>
}


impl Registers for StructRegisters {
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

    fn l(&self) -> Register<Word> {
        Register(self.hl().value().low_word())
    }
}

#[test]
fn should_get_value_from_registers() {
    let regs = StructRegisters {
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