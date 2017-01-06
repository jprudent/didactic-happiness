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

pub trait AsString {
    fn to_string(&self, &ComputerUnit) -> String;
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

impl AsString for WordRegister {
    fn to_string(&self, _: &ComputerUnit) -> String {
        match *self {
            WordRegister::A => "a".to_string(),
            WordRegister::B => "b".to_string(),
            WordRegister::C => "c".to_string(),
            WordRegister::D => "d".to_string(),
            WordRegister::E => "e".to_string(),
            WordRegister::H => "h".to_string(),
            WordRegister::L => "l".to_string(),
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

impl AsString for DoubleRegister {
    fn to_string(&self, _: &ComputerUnit) -> String {
        match *self {
            DoubleRegister::AF => "af".to_string(),
            DoubleRegister::BC => "bc".to_string(),
            DoubleRegister::DE => "de".to_string(),
            DoubleRegister::HL => "hl".to_string(),
            DoubleRegister::SP => "sp".to_string(),
        }
    }
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

impl AsString for ImmediateWord {
    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:02X}", cpu.word_at(cpu.get_pc_register() + 1))
    }
}

impl RightOperand<Word> for ImmediateWord {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        cpu.word_at(cpu.get_pc_register() + 1)
    }
}

pub struct ImmediateDouble {}

impl AsString for ImmediateDouble {
    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:04X}", cpu.double_at(cpu.get_pc_register() + 1))
    }
}

impl RightOperand<Double> for ImmediateDouble {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Double {
        cpu.double_at(cpu.get_pc_register() + 1)
    }
}

pub struct HighMemoryPointer {}

impl HighMemoryPointer {
    fn relative(&self, cpu: &ComputerUnit) -> Word {
        cpu.word_at(cpu.get_pc_register() + 1)
    }
}

impl RightOperand<Word> for HighMemoryPointer {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        cpu.word_at(set_low_word(0xFF00, self.relative(cpu)))
    }
}

impl LeftOperand<Word> for HighMemoryPointer {
    fn alter(&self, cpu: &mut ComputerUnit, value: Word) {
        let relative = self.relative(cpu);
        cpu.set_word_at(set_low_word(0xFF00, relative), value)
    }
}

impl AsString for HighMemoryPointer {
    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("(ff00 + {:02X})", self.relative(cpu))
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

impl AsString for ImmediatePointer<Word> {
    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:02X}", self.address(cpu))
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

impl AsString for ImmediatePointer<Double> {
    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("{:04X}", self.address(cpu))
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

impl AsString for RegisterPointer {
    fn to_string(&self, _: &ComputerUnit) -> String {
        match *self {
            RegisterPointer::HL => "(hl)".to_string(),
            RegisterPointer::BC => "(bc)".to_string(),
            RegisterPointer::DE => "(de)".to_string(),
            RegisterPointer::C => "(ff00 + c)".to_string()
        }
    }
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

impl AsString for HlOp {
    fn to_string(&self, _: &ComputerUnit) -> String {
        match *self {
            HlOp::HLI => "(HLI)".to_string(),
            HlOp::HLD => "(HLD)".to_string()
        }
    }
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

impl AsString for SpRelative {
    fn to_string(&self, cpu: &ComputerUnit) -> String {
        format!("(sp + {:02X})", cpu.word_at(cpu.get_pc_register() + 1))
    }
}

impl RightOperand<Double> for SpRelative {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Double {
        use super::alu::ArithmeticLogicalUnit;
        let sp = cpu.get_sp_register();
        let pc = cpu.get_pc_register();
        let word = cpu.word_at(pc + 1);
        let r = ArithmeticLogicalUnit::add_16_8(sp, word, 0);
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

impl AsString for ConstantAddress {
    fn to_string(&self, _: &ComputerUnit) -> String {
        format!("{:04X}", self.0)
    }
}

//todo factorize with ContantAddress ?
//todo rename to Whatever
pub struct Constant(pub Word);

impl RightOperand<Word> for Constant {
    fn resolve(&self, _: &mut ComputerUnit) -> Word {
        self.0
    }
}

impl AsString for Constant {
    fn to_string(&self, _: &ComputerUnit) -> String {
        format!("{:02X}", self.0)
    }
}

pub struct Carry {}

impl AsString for Carry {
    fn to_string(&self, _: &ComputerUnit) -> String {
        "".to_string()
    }
}

impl RightOperand<Word> for Carry {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        if cpu.carry_flag() {
            1
        } else {
            0
        }
    }
}