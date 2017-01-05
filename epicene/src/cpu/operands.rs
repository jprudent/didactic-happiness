// todo ComputerUnit should be a trait (or not ?). Because struct has fielf visibility, it can be perceived as a type
use super::{Word, Double, Address, ComputerUnit, set_low_word};
use std::marker::PhantomData;

pub trait RightOperand<R> {
    // in some very rare case reading a value can mut the cpu
    // LD A,(HLI) (0x2A) mut HL
    // LD A,(HLD) (0x3A) mut HL
    // LD HL, SP+WW (0xF8) mut the flag register
    fn resolve(&self, cpu: &mut ComputerUnit) -> R;
}

pub trait LeftOperand<L> {
    fn alter(&self, cpu: &mut ComputerUnit, value: L);
}

pub enum WordRegister {
    A,
    B,
    C,
    D,
    E,
    H,
    L,
}

impl LeftOperand<Word> for WordRegister {
    fn alter(&self, cpu: &mut ComputerUnit, word: Word) {
        match *self {
            WordRegister::A => cpu.set_register_a(word),
            WordRegister::B => cpu.set_register_b(word),
            WordRegister::C => cpu.set_register_c(word),
            WordRegister::D => cpu.set_register_d(word),
            WordRegister::E => cpu.set_register_e(word),
            WordRegister::H => cpu.set_register_h(word),
            WordRegister::L => cpu.set_register_l(word),
        }
    }
}

impl RightOperand<Word> for WordRegister {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        match *self {
            WordRegister::A => cpu.get_a_register(),
            WordRegister::B => cpu.get_b_register(),
            WordRegister::C => cpu.get_c_register(),
            WordRegister::D => cpu.get_d_register(),
            WordRegister::E => cpu.get_e_register(),
            WordRegister::H => cpu.get_h_register(),
            WordRegister::L => cpu.get_l_register(),
        }
    }
}

pub enum DoubleRegister {
    AF,
    BC,
    DE,
    HL,
    SP
}

impl RightOperand<Double> for DoubleRegister {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Double {
        match *self {
            DoubleRegister::AF => cpu.get_af_register(),
            DoubleRegister::BC => cpu.get_bc_register(),
            DoubleRegister::DE => cpu.get_de_register(),
            DoubleRegister::HL => cpu.get_hl_register(),
            DoubleRegister::SP => cpu.get_sp_register(),
        }
    }
}

impl LeftOperand<Double> for DoubleRegister {
    fn alter(&self, cpu: &mut ComputerUnit, double: Double) {
        match *self {
            DoubleRegister::AF => cpu.set_register_af(double),
            DoubleRegister::BC => cpu.set_register_bc(double),
            DoubleRegister::DE => cpu.set_register_de(double),
            DoubleRegister::HL => cpu.set_register_hl(double),
            DoubleRegister::SP => cpu.set_register_sp(double),
        }
    }
}


// TODO generic ?
pub struct ImmediateWord {}

impl RightOperand<Word> for ImmediateWord {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        cpu.word_at(cpu.get_pc_register() + 1)
    }
}

pub struct ImmediateDouble {}

impl RightOperand<Double> for ImmediateDouble {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Double {
        cpu.double_at(cpu.get_pc_register() + 1)
    }
}

pub struct HighMemoryPointer {}

impl RightOperand<Word> for HighMemoryPointer {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        let relative = cpu.word_at(cpu.get_pc_register() + 1);
        cpu.word_at(set_low_word(0xFF00, relative))
    }
}

impl LeftOperand<Word> for HighMemoryPointer {
    fn alter(&self, cpu: &mut ComputerUnit, value: Word) {
        let relative = cpu.word_at(cpu.get_pc_register() + 1);
        cpu.set_word_at(set_low_word(0xFF00, relative), value)
    }
}

pub struct ImmediatePointer<T> {
    resource_type: PhantomData<T>,
}

impl ImmediatePointer<Word> {
    pub fn new() -> ImmediatePointer<Word> {
        ImmediatePointer {
            resource_type: PhantomData
        }
    }

    fn address(&self, cpu: &ComputerUnit) -> Address {
        cpu.double_at(cpu.get_pc_register() + 1)
    }
}

impl LeftOperand<Word> for ImmediatePointer<Word> {
    fn alter(&self, cpu: &mut ComputerUnit, word: Word) {
        let address = self.address(cpu);
        cpu.set_word_at(address, word);
    }
}

impl RightOperand<Word> for ImmediatePointer<Word> {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        cpu.word_at(self.address(cpu))
    }
}

impl ImmediatePointer<Double> {
    pub fn new() -> ImmediatePointer<Double> {
        ImmediatePointer {
            resource_type: PhantomData
        }
    }

    fn address(&self, cpu: &ComputerUnit) -> Address {
        cpu.double_at(cpu.get_pc_register() + 1)
    }
}

impl LeftOperand<Double> for ImmediatePointer<Double> {
    fn alter(&self, cpu: &mut ComputerUnit, double: Double) {
        let address = self.address(cpu);
        cpu.set_double_at(address, double);
    }
}

impl RightOperand<Double> for ImmediatePointer<Double> {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Double {
        cpu.double_at(self.address(cpu))
    }
}

pub enum RegisterPointer {
    HL,
    BC,
    DE,
    C
}

impl LeftOperand<Word> for RegisterPointer {
    fn alter(&self, cpu: &mut ComputerUnit, word: Word) {
        let address = match *self {
            RegisterPointer::HL => cpu.get_hl_register(),
            RegisterPointer::BC => cpu.get_bc_register(),
            RegisterPointer::DE => cpu.get_de_register(),
            RegisterPointer::C => set_low_word(0xFF00, cpu.get_c_register())
        };
        cpu.set_word_at(address, word)
    }
}

impl RightOperand<Word> for RegisterPointer {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        let address = match *self {
            RegisterPointer::HL => cpu.get_hl_register(),
            RegisterPointer::BC => cpu.get_bc_register(),
            RegisterPointer::DE => cpu.get_de_register(),
            RegisterPointer::C => set_low_word(0xFF00, cpu.get_c_register()),
        };
        cpu.word_at(address)
    }
}

pub enum HlOp {
    HLI,
    HLD
}

impl HlOp {
    fn apply(&self, double: Double) -> Double {
        match *self {
            HlOp::HLI => double + 1,
            HlOp::HLD => double - 1
        }
    }
}

impl LeftOperand<Word> for HlOp {
    fn alter(&self, cpu: &mut ComputerUnit, value: Word) {
        let hl = cpu.get_hl_register();
        cpu.set_word_at(hl, value);
        cpu.set_register_hl(self.apply(hl))
    }
}

impl RightOperand<Word> for HlOp {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        let ret = cpu.word_at(cpu.get_hl_register());
        let hl = cpu.get_hl_register();
        //this is the only known case where right operand mut the cpu
        cpu.set_register_hl(self.apply(hl));
        ret
    }
}

pub struct SpRelative {}

impl RightOperand<Double> for SpRelative {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Double {
        use super::alu::ArithmeticLogicalUnit;
        let sp = cpu.get_sp_register();
        let pc = cpu.get_pc_register();
        let word = cpu.word_at(pc + 1);
        let r = ArithmeticLogicalUnit::add16(sp, word);
        r.flags().set_flags(cpu); // mut the CPU
        r.result()
    }
}

pub struct ConstantAddress(pub Double);

impl RightOperand<Double> for ConstantAddress {
    fn resolve(&self, _: &mut ComputerUnit) -> Double {
        self.0
    }
}

//todo factorize with ContantAddress ?
pub struct Constant(pub Word);

impl RightOperand<Word> for Constant {
    fn resolve(&self, _: &mut ComputerUnit) -> Word {
        self.0
    }
}

pub struct Carry {}

impl RightOperand<Word> for Carry {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        if cpu.carry_flag() {
            1
        } else {
            0
        }
    }
}