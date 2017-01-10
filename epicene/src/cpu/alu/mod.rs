use super::{Word, Double, ComputerUnit, low_word, set_low_word};

pub struct ArithmeticLogicalUnit {}

#[derive(Debug, PartialEq, Eq)]
pub struct ArithmeticResult<X> {
    result: X,
    flags: FlagRegister
}

impl<X: Copy> ArithmeticResult<X> {
    pub fn result(&self) -> X {
        self.result
    }

    pub fn flags(&self) -> &FlagRegister {
        &self.flags
    }
}

// todo this struct still needed ? can inline fields in ArithmeticResult
#[derive(Debug, PartialEq, Eq)]
pub struct FlagRegister {
    zf: bool,
    n: bool,
    h: bool,
    cy: bool
}

impl FlagRegister {
    pub fn zero_flag(&self) -> bool {
        self.zf
    }

    pub fn carry_flag(&self) -> bool {
        self.cy
    }

    pub fn half_carry_flag(&self) -> bool {
        self.h
    }

    pub fn add_sub_flag(&self) -> bool {
        self.n
    }

    pub fn set_flags(&self, cpu: &mut ComputerUnit) {
        cpu.set_zero_flag(self.zero_flag());
        cpu.set_add_sub_flag(self.add_sub_flag());
        cpu.set_carry_flag(self.carry_flag());
        cpu.set_half_carry_flag(self.half_carry_flag());
    }
}

impl ArithmeticLogicalUnit {
    // TODO unused parameters
    pub fn add(a: Word, b: Word, _: Word) -> ArithmeticResult<Word> {
        let result = a.wrapping_add(b);
        ArithmeticResult {
            result: result,
            flags: FlagRegister {
                cy: ArithmeticLogicalUnit::has_carry(a, b),
                h: ArithmeticLogicalUnit::has_half_carry(a, b),
                zf: result == 0,
                n: false
            }
        }
    }

    // TODO unused parameters
    pub fn add_16_16(a: Double, b: Double, _: Word) -> ArithmeticResult<Double> {
        let result = a.wrapping_add(b);
        ArithmeticResult {
            result: result,
            flags: FlagRegister {
                cy: ArithmeticLogicalUnit::has_carry_16(a, b),
                h: ArithmeticLogicalUnit::has_half_carry_16(a, b),
                zf: false,
                n: false
            }
        }
    }

    pub fn inc(a: Word) -> ArithmeticResult<Word> {
        // TODO move the logic of caller here
        ArithmeticLogicalUnit::add(a, 1, 0)
    }

    pub fn dec(a: Word) -> ArithmeticResult<Word> {
        // TODO move the logic of caller here
        ArithmeticLogicalUnit::sub(a, 1, 0)
    }


    fn has_carry_16(a: Double, b: Double) -> bool {
        let overflowing_result: u32 = a as u32 + b as u32;
        (overflowing_result & 0x10000) != 0
    }

    fn has_half_carry_16(a: Double, b: Double) -> bool {
        let ah = a & 0x0FFF;
        let bh = b & 0x0FFF;
        let add = ah + bh;
        (add & 0x1000) != 0
    }

    fn is_negative(word: Word) -> bool {
        word & 0x80 != 0
    }

    // TODO unused parameters
    pub fn add_16_8_signed(a: Double, b: Word, _: Word) -> ArithmeticResult<Double> {
        let result =
        if ArithmeticLogicalUnit::is_negative(b) {
            let negative = (!b).wrapping_add(1);
            a.wrapping_sub(negative as Double)
        } else {
            a.wrapping_add(b as Double)
        };
        ArithmeticResult {
            result: result,
            flags: FlagRegister {
                cy: ArithmeticLogicalUnit::has_carry(low_word(a), b),
                h: ArithmeticLogicalUnit::has_half_carry(low_word(a), b),
                zf: false,
                n: false
            }
        }
    }

    // TODO unused parameters
    pub fn sub(a: Word, b: Word, _: Word) -> ArithmeticResult<Word> {
        let two_complement = (!b).wrapping_add(1);
        let mut add = ArithmeticLogicalUnit::add(a, two_complement, 0);
        add.flags.n = true;
        add.flags.cy = !add.flags.cy;
        add.flags.h = !add.flags.h;
        add
    }

    // TODO unused parameters
    pub fn and(a: Word, b: Word, _: Word) -> ArithmeticResult<Word> {
        let r = a & b;
        ArithmeticResult {
            result: r,
            flags: FlagRegister {
                zf: r == 0,
                n: false,
                h: true,
                cy: false
            }
        }
    }

    // TODO unused parameters
    pub fn or(a: Word, b: Word, _: Word) -> ArithmeticResult<Word> {
        let r = a | b;
        ArithmeticResult {
            result: r,
            flags: FlagRegister {
                zf: r == 0,
                n: false,
                h: false,
                cy: false
            }
        }
    }

    // TODO unused parameters
    pub fn rotate_left(a: Word, _: Word, _: Word) -> ArithmeticResult<Word> {
        let r = a.rotate_left(1);
        ArithmeticResult {
            result: r,
            flags: FlagRegister {
                zf: r == 0,
                n: false,
                h: false,
                cy: (r & 0x80) == 1
            }
        }
    }

    // TODO unused parameters
    pub fn rotate_right(a: Word, _: Word, _: Word) -> ArithmeticResult<Word> {
        let r = a.rotate_right(1);
        ArithmeticResult {
            result: r,
            flags: FlagRegister {
                zf: r == 0,
                n: false,
                h: false,
                cy: (r & 1) == 1
            }
        }
    }

    // TODO unused parameters
    pub fn rotate_right_through_carry(a: Word, _: Word, carry: Word) -> ArithmeticResult<Word> {
        assert!(carry <= 1, "Carry should be 1 or 0");
        let c = carry.wrapping_shl(7);
        let r = (a.rotate_right(1) & 0x7F) | c;
        ArithmeticResult {
            result: r,
            flags: FlagRegister {
                zf: r == 0,
                n: false,
                h: false,
                cy: a & 1 != 0
            }
        }
    }

    // TODO unused parameters
    pub fn rotate_left_through_carry(a: Word, _: Word, carry: Word) -> ArithmeticResult<Word> {
        assert!(carry <= 1, "Carry should be 1 or 0");
        let r = (a.rotate_left(1) & 0xFE) | carry;
        ArithmeticResult {
            result: r,
            flags: FlagRegister {
                zf: r == 0,
                n: false,
                h: false,
                cy: (a & 0x80) != 0
            }
        }
    }

    // TODO unused parameters
    pub fn shift_right(a: Word, _: Word, _: Word) -> ArithmeticResult<Word> {
        let r = a.wrapping_shr(1);
        ArithmeticResult {
            result: r,
            flags: FlagRegister {
                zf: r == 0,
                n: false,
                h: false,
                cy: a & 1 != 0
            }
        }
    }

    // TODO unused parameters
    pub fn shift_left(a: Word, _: Word, _: Word) -> ArithmeticResult<Word> {
        let r = a.wrapping_shl(1);
        ArithmeticResult {
            result: r,
            flags: FlagRegister {
                zf: r == 0,
                n: false,
                h: false,
                cy: a & 0x80 != 0
            }
        }
    }

    // TODO unused parameters
    pub fn swap(a: Word, _: Word, _: Word) -> ArithmeticResult<Word> {
        let r = a.rotate_right(4);
        ArithmeticResult {
            result: r,
            flags: FlagRegister {
                zf: r == 0,
                n: false,
                h: false,
                cy: false
            }
        }
    }

    // TODO check implementation, really not sure about it
    pub fn add_with_carry(a: Word, b: Word, carry: Word) -> ArithmeticResult<Word> {
        assert!(carry <= 1, "carry should be 0 or 1");
        let r1 = ArithmeticLogicalUnit::add(b, carry, 0);
        ArithmeticLogicalUnit::add(a, r1.result(), 0)
    }

    pub fn sub_with_carry(a: Word, b: Word, carry: Word) -> ArithmeticResult<Word> {
        assert!(carry <= 1, "carry should be 0 or 1");
        let r1 = ArithmeticLogicalUnit::add(b, carry, 0);
        ArithmeticLogicalUnit::sub(a, r1.result(), 0)
    }

    fn has_carry(a: Word, b: Word) -> bool {
        let overflowing_result: u16 = a as u16 + b as u16;
        (overflowing_result & 0x0100) != 0
    }

    fn has_half_carry(a: Word, b: Word) -> bool {
        let nibble = ArithmeticLogicalUnit::low_nibble(a) + ArithmeticLogicalUnit::low_nibble(b);
        (nibble & 0x10) != 0
    }

    fn low_nibble(a: Word) -> u8 {
        a & 0xF
    }
}

#[test]
fn should_add() {
    assert_eq!(ArithmeticLogicalUnit::add(1, 1, 0), ArithmeticResult {
        result: 0b10,
        flags: FlagRegister {
            cy: false,
            h: false,
            zf: false,
            n: false
        }
    });
    assert_eq!(ArithmeticLogicalUnit::add(0b1000, 0b1000, 0), ArithmeticResult {
        result: 0b10000,
        flags: FlagRegister {
            cy: false,
            h: true,
            zf: false,
            n: false
        }
    });
    assert_eq!(ArithmeticLogicalUnit::add(0b1000_0000, 0b1000_0000, 0), ArithmeticResult {
        result: 0,
        flags: FlagRegister {
            cy: true,
            h: false,
            zf: true,
            n: false
        }
    });
    assert_eq!(ArithmeticLogicalUnit::add(0b1111_1000, 0b1000, 0), ArithmeticResult {
        result: 0,
        flags: FlagRegister {
            cy: true,
            h: true,
            zf: true,
            n: false
        }
    });

    assert_eq!(ArithmeticLogicalUnit::add(0b01, 0b1111_1110, 0).result, 0xFF);

    assert!(ArithmeticLogicalUnit::add(0xAA, 0xAA, 0).flags.cy);
}

#[test]
fn should_sub() {
    assert_eq!(ArithmeticLogicalUnit::sub(1, 1, 0), ArithmeticResult {
        result: 0,
        flags: FlagRegister {
            cy: false,
            h: false,
            zf: true,
            n: true
        }
    });
    assert_eq!(ArithmeticLogicalUnit::sub(0b01, 0b10, 0).result, ArithmeticLogicalUnit::add(0b01, 0b1111_1110, 0).result);
    assert_eq!(ArithmeticLogicalUnit::sub(0, 1, 0), ArithmeticResult {
        result: 0xFF,
        flags: FlagRegister {
            cy: true,
            h: true,
            zf: false,
            n: true
        }
    });

    assert_eq!(ArithmeticLogicalUnit::sub(0x90, 0x92, 0), ArithmeticResult {
        result: 0xFE,
        flags: FlagRegister {
            cy: true,
            h: true,
            zf: false,
            n: true
        }
    })
}
