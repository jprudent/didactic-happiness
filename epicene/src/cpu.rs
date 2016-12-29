use std::ops::IndexMut;
use std::ops::Index;
use std::marker::PhantomData;

type Word = u8;
type Double = u16;
type Address = Double;
type Cycle = u8;
type Size = u16;

fn high_word(double: Double) -> Word {
    double.wrapping_shr(8) as Word
}

fn low_word(double: Double) -> Word {
    (double & 0xFF) as Word
}

fn set_high_word(double: Double, word: Word) -> Double {
    (double & 0xFF) | (word as Double).wrapping_shl(8)
}

fn set_low_word(double: Double, word: Word) -> Double {
    (double & 0xFF00) | (word as Double)
}

#[derive(Debug, PartialEq, Eq)]
struct FlagRegister {
    zf: bool,
    n: bool,
    h: bool,
    cy: bool
}

impl FlagRegister {
    fn new() -> FlagRegister {
        FlagRegister {
            zf: false,
            n: false,
            h: false,
            cy: false
        }
    }

    fn zero_flag(&self) -> bool {
        self.zf
    }

    fn carry_flag(&self) -> bool {
        self.cy
    }

    fn half_carry_flag(&self) -> bool {
        self.h
    }

    fn add_sub_flag(&self) -> bool {
        self.n
    }

    fn set_zero_flag(&mut self, flag_value: bool) {
        self.zf = flag_value
    }

    fn set_carry_flag(&mut self, flag_value: bool) {
        self.cy = flag_value
    }

    fn set_half_carry_flag(&mut self, flag_value: bool) {
        self.h = flag_value
    }

    fn set_add_sub_flag(&mut self, flag_value: bool) {
        self.n = flag_value
    }
}


pub struct Registers {
    af: Double,
    bc: Double,
    de: Double,
    hl: Double,
    sp: Double,
    pc: Double,
    flags: FlagRegister,
}

impl Registers {
    pub fn new() -> Registers {
        Registers {
            af: 0x1234,
            bc: 0x1234,
            de: 0x1234,
            hl: 0x1234,
            sp: 0x1234,
            pc: 0x0000,
            flags: FlagRegister::new()
        }
    }

    fn a(&self) -> Word {
        high_word(self.af)
    }

    fn b(&self) -> Word {
        high_word(self.bc)
    }

    fn c(&self) -> Word {
        low_word(self.bc)
    }

    fn d(&self) -> Word {
        high_word(self.de)
    }

    fn e(&self) -> Word {
        low_word(self.de)
    }

    fn h(&self) -> Word {
        high_word(self.hl)
    }

    fn l(&self) -> Word {
        low_word(self.hl)
    }

    fn zero_flag(&self) -> bool {
        self.flags.zero_flag()
    }

    fn carry_flag(&self) -> bool {
        self.flags.carry_flag()
    }

    fn half_carry_flag(&self) -> bool {
        self.flags.half_carry_flag()
    }

    fn add_sub_flag(&self) -> bool {
        self.flags.add_sub_flag()
    }

    fn set_zero_flag(&mut self, flag_value: bool) {
        self.flags.set_zero_flag(flag_value)
    }

    fn set_carry_flag(&mut self, flag_value: bool) {
        self.flags.set_carry_flag(flag_value)
    }

    fn set_half_carry_flag(&mut self, flag_value: bool) {
        self.flags.set_half_carry_flag(flag_value)
    }

    fn set_add_sub_flag(&mut self, flag_value: bool) {
        self.flags.set_add_sub_flag(flag_value)
    }
}


#[test]
fn should_get_value_from_registers() {
    let regs = Registers {
        af: 0xAAFF,
        bc: 0xBBCC,
        de: 0xDDEE,
        hl: 0x4411,
        sp: 0x5678,
        pc: 0x8765,
        flags: FlagRegister::new()
    };
    assert_eq!(regs.af, 0xAAFF);
    assert_eq!(regs.b(), 0xBB);
    assert_eq!(regs.c(), 0xCC);
}

struct Load<X, L: LeftOperand<X>, R: RightOperand<X>> {
    destination: L,
    source: R,
    size: Double,
    cycles: Cycle,
    operation_type: PhantomData<X>
}

impl<X, L: LeftOperand<X>, R: RightOperand<X>> Load<X, L, R> {
    fn ldh_ptr_a() -> Load<Word, HighMemoryPointer, WordRegister> {
        Load {
            destination: HighMemoryPointer {},
            source: WordRegister::A,
            size: 2,
            cycles: 12,
            operation_type: PhantomData
        }
    }

    fn ldh_a_ptr() -> Load<Word, WordRegister, HighMemoryPointer> {
        Load {
            destination: WordRegister::A,
            source: HighMemoryPointer {},
            size: 2,
            cycles: 12,
            operation_type: PhantomData
        }
    }
}

enum WordRegister {
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

enum DoubleRegister {
    BC,
    DE,
    HL,
    SP
}

impl RightOperand<Double> for DoubleRegister {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Double {
        match *self {
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
            DoubleRegister::BC => cpu.set_register_bc(double),
            DoubleRegister::DE => cpu.set_register_de(double),
            DoubleRegister::HL => cpu.set_register_hl(double),
            DoubleRegister::SP => cpu.set_register_sp(double),
        }
    }
}

trait RightOperand<R> {
    // in some very rare case reading a value can mut the cpu
    // LD A,(HLI) (0x2A)
    // LD A,(HLD) (0x3A)
    fn resolve(&self, cpu: &mut ComputerUnit) -> R;
}

trait LeftOperand<L> {
    fn alter(&self, cpu: &mut ComputerUnit, value: L);
}

// TODO generic ?
struct ImmediateWord {}

impl RightOperand<Word> for ImmediateWord {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Word {
        cpu.word_at(cpu.get_pc_register() + 1)
    }
}

struct ImmediateDouble {}

impl RightOperand<Double> for ImmediateDouble {
    fn resolve(&self, cpu: &mut ComputerUnit) -> Double {
        cpu.double_at(cpu.get_pc_register() + 1)
    }
}

struct HighMemoryPointer {}

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

struct ImmediatePointer<T> {
    resource_type: PhantomData<T>,
}

impl ImmediatePointer<Word> {
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

enum RegisterPointer {
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

impl<X, L: LeftOperand<X>, R: RightOperand<X>> Opcode for Load<X, L, R> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let word = self.source.resolve(cpu);
        self.destination.alter(cpu, word);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}

trait Opcode {
    fn exec(&self, cpu: &mut ComputerUnit);
    fn size(&self) -> Size;
    fn cycles(&self, &ComputerUnit) -> Cycle;
}

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
}

struct Nop {
    size: Size,
    cycles: Cycle
}

impl Opcode for Nop {
    fn exec(&self, _: &mut ComputerUnit) {}

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}


struct UnconditionalJump<X, A: RightOperand<X>> {
    address: A,
    size: Size,
    cycles: Cycle,
    operation_type: PhantomData<X>
}

impl<A: RightOperand<Double>> Opcode for UnconditionalJump<Double, A> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let address: Double = self.address.resolve(cpu);
        cpu.set_register_pc(address - self.size()); // self.size() is added afterward
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}

struct UnconditionalCall {
    address: ImmediateDouble,
    size: Size,
    cycles: Cycle
}

impl UnconditionalCall {
    fn new() -> UnconditionalCall {
        UnconditionalCall {
            address: ImmediateDouble {},
            size: 3,
            cycles: 24
        }
    }
}

impl Opcode for UnconditionalCall {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let pc = cpu.get_pc_register();
        cpu.push(pc);

        let address = self.address.resolve(cpu);
        cpu.set_register_pc(address - self.size()); // self.size() is added afterward
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}

#[derive(Debug)]
enum JmpCondition {
    NONZERO,
    //ZERO,
    NOCARRY,
    //CARRY
}

impl JmpCondition {
    fn matches(&self, cpu: &ComputerUnit) -> bool {
        match *self {
            JmpCondition::NONZERO => !cpu.zero_flag(),
            //JmpCondition::ZERO => cpu.zero_flag(),
            JmpCondition::NOCARRY => !cpu.carry_flag(),
            //JmpCondition::CARRY => cpu.carry_flag()
        }
    }
}

struct ConditionalReturn {
    condition: JmpCondition,
    size: Size,
    cycles_when_taken: Cycle,
    cycles_when_not_taken: Cycle
}

impl ConditionalReturn {
    fn ret_nc() -> ConditionalReturn {
        ConditionalReturn {
            condition: JmpCondition::NOCARRY,
            size: 1,
            cycles_when_taken: 20,
            cycles_when_not_taken: 8
        }
    }
}

impl Opcode for ConditionalReturn {
    fn exec(&self, cpu: &mut ComputerUnit) {
        if self.condition.matches(cpu) {
            let address = cpu.pop();
            cpu.set_register_pc(address - self.size())
        }
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, cpu: &ComputerUnit) -> Cycle {
        if self.condition.matches(cpu) {
            self.cycles_when_taken
        } else {
            self.cycles_when_not_taken
        }
    }
}

struct ConditionalJump<X, A: RightOperand<X>> {
    address: A,
    condition: JmpCondition,
    size: Size,
    cycles_when_taken: Cycle,
    cycles_when_not_taken: Cycle,
    operation_type: PhantomData<X>
}

fn is_negative(word: Word) -> bool {
    word & 0x80 != 0
}

impl<A: RightOperand<Word>> Opcode for ConditionalJump<Word, A> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let relative_address: Word = self.address.resolve(cpu);
        if self.condition.matches(cpu) {
            //don't use ALU because we don't touch flags
            let address = if is_negative(relative_address) {
                let negative_address = (!relative_address).wrapping_sub(1);
                cpu.get_pc_register().wrapping_sub(negative_address as u16)
            } else {
                cpu.get_pc_register().wrapping_add(relative_address as u16)
            };
            cpu.set_register_pc(address - self.size()); // self.size() is added afterward
        }
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, cpu: &ComputerUnit) -> Cycle {
        if self.condition.matches(cpu) {
            self.cycles_when_taken
        } else {
            self.cycles_when_not_taken
        }
    }
}

struct DisableInterrupts {
    size: Size,
    cycles: Cycle
}

impl Opcode for DisableInterrupts {
    fn exec(&self, cpu: &mut ComputerUnit) {
        cpu.disable_interrupt_master()
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}

struct XorWithA<S: RightOperand<Word>> {
    source: S,
    size: Size,
    cycles: Cycle
}

impl<S: RightOperand<Word>> Opcode for XorWithA<S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let n = self.source.resolve(cpu);
        let a = cpu.get_a_register();
        let r = a ^ n;
        cpu.set_register_a(r);
        cpu.set_zero_flag(r == 0);
        cpu.set_carry_flag(false);
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


struct Decoder(Vec<Box<Opcode>>);

impl Decoder {
    fn push<T: 'static + Opcode>(&mut self, opcode: T) {
        self.0.push(Box::new(opcode))
    }
}

impl Index<Word> for Decoder {
    type Output = Box<Opcode>;

    fn index(&self, index: Word) -> &Self::Output {
        &self.0[index as usize]
    }
}

impl IndexMut<Word> for Decoder {
    fn index_mut(&mut self, index: Word) -> &mut Self::Output {
        &mut self.0[index as usize]
    }
}

fn build_decoder() -> Decoder {
    fn ld_ptr_r_from_r(destination: RegisterPointer, source: WordRegister) -> Box<Load<Word, RegisterPointer, WordRegister>> {
        Box::new(Load {
            destination: destination,
            source: source,
            size: 1,
            cycles: 8,
            operation_type: PhantomData
        })
    }

    fn ld_r_from_w(destination: WordRegister) -> Box<Load<Word, WordRegister, ImmediateWord>> {
        Box::new(Load {
            destination: destination,
            source: ImmediateWord {},
            size: 2,
            cycles: 8,
            operation_type: PhantomData
        })
    }
    fn ld_rr_from_ww(destination: DoubleRegister) -> Box<Load<Double, DoubleRegister, ImmediateDouble>> {
        Box::new(Load {
            destination: destination,
            source: ImmediateDouble {},
            size: 3,
            cycles: 12,
            operation_type: PhantomData
        })
    }

    fn ld_r_from_ptr_r(destination: WordRegister, source: RegisterPointer) -> Box<Load<Word, WordRegister, RegisterPointer>> {
        Box::new(Load {
            destination: destination,
            source: source,
            size: 1,
            cycles: 8,
            operation_type: PhantomData
        })
    }

    fn ld_r_from_r(destination: WordRegister, source: WordRegister) -> Box<Load<Word, WordRegister, WordRegister>> {
        Box::new(Load {
            destination: destination,
            source: source,
            size: 1,
            cycles: 4,
            operation_type: PhantomData
        })
    }

    fn ld_ptr_nn_from_rr(source: DoubleRegister) -> Box<Load<Double, ImmediatePointer<Double>, DoubleRegister>> {
        Box::new(Load {
            destination: ImmediatePointer { resource_type: PhantomData },
            source: source,
            size: 3,
            cycles: 20,
            operation_type: PhantomData
        })
    }

    fn ld_ptr_nn_from_r(source: WordRegister) -> Box<Load<Word, ImmediatePointer<Word>, WordRegister>> {
        Box::new(Load {
            destination: ImmediatePointer { resource_type: PhantomData },
            source: source,
            size: 3,
            cycles: 16,
            operation_type: PhantomData
        })
    }

    fn ld_ptr_r_from_w(destination: RegisterPointer) -> Box<Load<Word, RegisterPointer, ImmediateWord>> {
        Box::new(Load {
            destination: destination,
            source: ImmediateWord {},
            size: 1,
            cycles: 12,
            operation_type: PhantomData
        })
    }

    fn ld_rr_from_rr(destination: DoubleRegister, source: DoubleRegister) -> Box<Load<Double, DoubleRegister, DoubleRegister>> {
        Box::new(Load {
            destination: destination,
            source: source,
            size: 1,
            cycles: 8,
            operation_type: PhantomData
        })
    }

    fn ld_r_from_ptr_nn(destination: WordRegister) -> Box<Load<Word, WordRegister, ImmediatePointer<Word>>> {
        Box::new(Load {
            destination: destination,
            source: ImmediatePointer { resource_type: PhantomData },
            size: 3,
            cycles: 16,
            operation_type: PhantomData
        })
    }

    fn nop() -> Box<Nop> {
        Box::new(Nop {
            size: 1,
            cycles: 4
        })
    }

    fn jmp_nn() -> Box<UnconditionalJump<Double, ImmediateDouble>> {
        Box::new(UnconditionalJump {
            address: ImmediateDouble {},
            size: 3,
            cycles: 16,
            operation_type: PhantomData
        })
    }

    fn jr_nz_w() -> Box<ConditionalJump<Word, ImmediateWord>> {
        Box::new(ConditionalJump {
            address: ImmediateWord {},
            condition: JmpCondition::NONZERO,
            size: 2,
            cycles_when_taken: 12,
            cycles_when_not_taken: 8,
            operation_type: PhantomData
        })
    }

    fn di() -> Box<DisableInterrupts> {
        Box::new(DisableInterrupts {
            size: 1,
            cycles: 4
        })
    }

    fn xor_r(r: WordRegister) -> Box<XorWithA<WordRegister>> {
        Box::new(XorWithA {
            source: r,
            size: 1,
            cycles: 4
        })
    }

    fn xor_ptr_r(r: RegisterPointer) -> Box<XorWithA<RegisterPointer>> {
        Box::new(XorWithA {
            source: r,
            size: 1,
            cycles: 8
        })
    }

    fn xor_n() -> Box<XorWithA<ImmediateWord>> {
        Box::new(XorWithA {
            source: ImmediateWord {},
            size: 2,
            cycles: 8
        })
    }

    fn ld_ptr_hl_from_a(hlop: HlOp) -> Box<Load<Word, HlOp, WordRegister>> {
        Box::new(Load {
            source: WordRegister::A,
            destination: hlop,
            size: 1,
            cycles: 8,
            operation_type: PhantomData
        })
    }

    fn ld_a_from_ptr_hl(hlop: HlOp) -> Box<Load<Word, WordRegister, HlOp>> {
        Box::new(Load {
            source: hlop,
            destination: WordRegister::A,
            size: 1,
            cycles: 8,
            operation_type: PhantomData
        })
    }

    fn sub_r(source: WordRegister) -> Box<ArithmeticOperationOnRegisterA<WordRegister>> {
        Box::new(ArithmeticOperationOnRegisterA {
            source: source,
            operation: ArithmeticLogicalUnit::sub,
            size: 1,
            cycles: 4
        })
    }

    fn sub_ptr_r(source: RegisterPointer) -> Box<ArithmeticOperationOnRegisterA<RegisterPointer>> {
        Box::new(ArithmeticOperationOnRegisterA {
            source: source,
            operation: ArithmeticLogicalUnit::sub,
            size: 1,
            cycles: 8
        })
    }

    fn dec_r(destination: WordRegister) -> Box<IncDec<WordRegister>> {
        Box::new(IncDec {
            destination: destination,
            operation: ArithmeticLogicalUnit::sub,
            size: 1,
            cycles: 4
        })
    }

    fn dec_ptr_r(destination: RegisterPointer) -> Box<IncDec<RegisterPointer>> {
        Box::new(IncDec {
            destination: destination,
            operation: ArithmeticLogicalUnit::sub,
            size: 1,
            cycles: 12
        })
    }

    fn add_r(source: WordRegister) -> Box<ArithmeticOperationOnRegisterA<WordRegister>> {
        Box::new(ArithmeticOperationOnRegisterA {
            source: source,
            operation: ArithmeticLogicalUnit::add,
            size: 1,
            cycles: 4
        })
    }

    fn add_ptr_r(source: RegisterPointer) -> Box<ArithmeticOperationOnRegisterA<RegisterPointer>> {
        Box::new(ArithmeticOperationOnRegisterA {
            source: source,
            operation: ArithmeticLogicalUnit::add,
            size: 1,
            cycles: 8
        })
    }

    fn inc_r(destination: WordRegister) -> Box<IncDec<WordRegister>> {
        Box::new(IncDec {
            destination: destination,
            operation: ArithmeticLogicalUnit::add,
            size: 1,
            cycles: 4
        })
    }

    fn inc_ptr_r(destination: RegisterPointer) -> Box<IncDec<RegisterPointer>> {
        Box::new(IncDec {
            destination: destination,
            operation: ArithmeticLogicalUnit::add,
            size: 1,
            cycles: 12
        })
    }

    let mut decoder = Decoder(vec!());

    //todo temp loop for growing the vec
    for i in 0..256 {
        decoder.push(NotImplemented(i as Word))
    }
    decoder[0x00] = nop();
    decoder[0x01] = ld_rr_from_ww(DoubleRegister::BC);
    decoder[0x02] = ld_ptr_r_from_r(RegisterPointer::BC, WordRegister::A);
    decoder[0x04] = inc_r(WordRegister::B);
    decoder[0x05] = dec_r(WordRegister::B);
    decoder[0x06] = ld_r_from_w(WordRegister::B);
    decoder[0x08] = ld_ptr_nn_from_rr(DoubleRegister::SP);
    decoder[0x0A] = ld_r_from_ptr_r(WordRegister::A, RegisterPointer::BC);
    decoder[0x0C] = inc_r(WordRegister::C);
    decoder[0x0D] = dec_r(WordRegister::C);
    decoder[0x0E] = ld_r_from_w(WordRegister::C);
    decoder[0x11] = ld_rr_from_ww(DoubleRegister::DE);
    decoder[0x12] = ld_ptr_r_from_r(RegisterPointer::DE, WordRegister::A);
    decoder[0x14] = inc_r(WordRegister::D);
    decoder[0x15] = dec_r(WordRegister::D);
    decoder[0x16] = ld_r_from_w(WordRegister::D);
    decoder[0x1A] = ld_r_from_ptr_r(WordRegister::A, RegisterPointer::DE);
    decoder[0x1C] = inc_r(WordRegister::E);
    decoder[0x1D] = dec_r(WordRegister::E);
    decoder[0x1E] = ld_r_from_w(WordRegister::E);
    decoder[0x20] = jr_nz_w();
    decoder[0x21] = ld_rr_from_ww(DoubleRegister::HL);
    decoder[0x22] = ld_ptr_hl_from_a(HlOp::HLI);
    decoder[0x24] = inc_r(WordRegister::H);
    decoder[0x25] = dec_r(WordRegister::H);
    decoder[0x26] = ld_r_from_w(WordRegister::H);
    decoder[0x2A] = ld_a_from_ptr_hl(HlOp::HLI);
    decoder[0x2C] = inc_r(WordRegister::L);
    decoder[0x2D] = dec_r(WordRegister::L);
    decoder[0x2E] = ld_r_from_w(WordRegister::L);
    decoder[0x31] = ld_rr_from_ww(DoubleRegister::SP);
    decoder[0x32] = ld_ptr_hl_from_a(HlOp::HLD);
    decoder[0x34] = inc_ptr_r(RegisterPointer::HL);
    decoder[0x35] = dec_ptr_r(RegisterPointer::HL);
    decoder[0x36] = ld_ptr_r_from_w(RegisterPointer::HL);
    decoder[0x3A] = ld_a_from_ptr_hl(HlOp::HLD);
    decoder[0x3C] = inc_r(WordRegister::A);
    decoder[0x3D] = dec_r(WordRegister::A);
    decoder[0x3E] = ld_r_from_w(WordRegister::A);
    decoder[0x40] = ld_r_from_r(WordRegister::B, WordRegister::B);
    decoder[0x41] = ld_r_from_r(WordRegister::B, WordRegister::C);
    decoder[0x42] = ld_r_from_r(WordRegister::B, WordRegister::D);
    decoder[0x43] = ld_r_from_r(WordRegister::B, WordRegister::E);
    decoder[0x44] = ld_r_from_r(WordRegister::B, WordRegister::H);
    decoder[0x45] = ld_r_from_r(WordRegister::B, WordRegister::L);
    decoder[0x46] = ld_r_from_ptr_r(WordRegister::B, RegisterPointer::HL);
    decoder[0x47] = ld_r_from_r(WordRegister::B, WordRegister::A);
    decoder[0x48] = ld_r_from_r(WordRegister::C, WordRegister::B);
    decoder[0x49] = ld_r_from_r(WordRegister::C, WordRegister::C);
    decoder[0x4A] = ld_r_from_r(WordRegister::C, WordRegister::D);
    decoder[0x4B] = ld_r_from_r(WordRegister::C, WordRegister::E);
    decoder[0x4C] = ld_r_from_r(WordRegister::C, WordRegister::H);
    decoder[0x4D] = ld_r_from_r(WordRegister::C, WordRegister::L);
    decoder[0x4E] = ld_r_from_ptr_r(WordRegister::C, RegisterPointer::HL);
    decoder[0x4F] = ld_r_from_r(WordRegister::C, WordRegister::A);
    decoder[0x50] = ld_r_from_r(WordRegister::D, WordRegister::B);
    decoder[0x51] = ld_r_from_r(WordRegister::D, WordRegister::C);
    decoder[0x52] = ld_r_from_r(WordRegister::D, WordRegister::D);
    decoder[0x53] = ld_r_from_r(WordRegister::D, WordRegister::E);
    decoder[0x54] = ld_r_from_r(WordRegister::D, WordRegister::H);
    decoder[0x55] = ld_r_from_r(WordRegister::D, WordRegister::L);
    decoder[0x56] = ld_r_from_ptr_r(WordRegister::D, RegisterPointer::HL);
    decoder[0x57] = ld_r_from_r(WordRegister::D, WordRegister::A);
    decoder[0x58] = ld_r_from_r(WordRegister::E, WordRegister::B);
    decoder[0x59] = ld_r_from_r(WordRegister::E, WordRegister::C);
    decoder[0x5A] = ld_r_from_r(WordRegister::E, WordRegister::D);
    decoder[0x5B] = ld_r_from_r(WordRegister::E, WordRegister::E);
    decoder[0x5C] = ld_r_from_r(WordRegister::E, WordRegister::H);
    decoder[0x5D] = ld_r_from_r(WordRegister::E, WordRegister::L);
    decoder[0x5E] = ld_r_from_ptr_r(WordRegister::E, RegisterPointer::HL);
    decoder[0x5F] = ld_r_from_r(WordRegister::E, WordRegister::A);
    decoder[0x60] = ld_r_from_r(WordRegister::H, WordRegister::B);
    decoder[0x61] = ld_r_from_r(WordRegister::H, WordRegister::C);
    decoder[0x62] = ld_r_from_r(WordRegister::H, WordRegister::D);
    decoder[0x63] = ld_r_from_r(WordRegister::H, WordRegister::E);
    decoder[0x64] = ld_r_from_r(WordRegister::H, WordRegister::H);
    decoder[0x65] = ld_r_from_r(WordRegister::H, WordRegister::L);
    decoder[0x67] = ld_r_from_r(WordRegister::H, WordRegister::A);
    decoder[0x68] = ld_r_from_r(WordRegister::L, WordRegister::B);
    decoder[0x69] = ld_r_from_r(WordRegister::L, WordRegister::C);
    decoder[0x6A] = ld_r_from_r(WordRegister::L, WordRegister::D);
    decoder[0x6B] = ld_r_from_r(WordRegister::L, WordRegister::E);
    decoder[0x6C] = ld_r_from_r(WordRegister::L, WordRegister::H);
    decoder[0x6D] = ld_r_from_r(WordRegister::L, WordRegister::L);
    decoder[0x6F] = ld_r_from_r(WordRegister::L, WordRegister::A);
    decoder[0x66] = ld_r_from_ptr_r(WordRegister::H, RegisterPointer::HL);
    decoder[0x6E] = ld_r_from_ptr_r(WordRegister::L, RegisterPointer::HL);
    decoder[0x70] = ld_ptr_r_from_r(RegisterPointer::HL, WordRegister::B);
    decoder[0x71] = ld_ptr_r_from_r(RegisterPointer::HL, WordRegister::C);
    decoder[0x72] = ld_ptr_r_from_r(RegisterPointer::HL, WordRegister::D);
    decoder[0x73] = ld_ptr_r_from_r(RegisterPointer::HL, WordRegister::E);
    decoder[0x74] = ld_ptr_r_from_r(RegisterPointer::HL, WordRegister::H);
    decoder[0x75] = ld_ptr_r_from_r(RegisterPointer::HL, WordRegister::L);
    decoder[0x77] = ld_ptr_r_from_r(RegisterPointer::HL, WordRegister::A);
    decoder[0x7E] = ld_r_from_ptr_r(WordRegister::A, RegisterPointer::HL);
    decoder[0x7F] = ld_r_from_r(WordRegister::A, WordRegister::A);
    decoder[0x78] = ld_r_from_r(WordRegister::A, WordRegister::B);
    decoder[0x79] = ld_r_from_r(WordRegister::A, WordRegister::C);
    decoder[0x7A] = ld_r_from_r(WordRegister::A, WordRegister::D);
    decoder[0x7B] = ld_r_from_r(WordRegister::A, WordRegister::E);
    decoder[0x7C] = ld_r_from_r(WordRegister::A, WordRegister::H);
    decoder[0x7D] = ld_r_from_r(WordRegister::A, WordRegister::L);
    decoder[0x7F] = ld_r_from_r(WordRegister::A, WordRegister::A);
    decoder[0x80] = add_r(WordRegister::B);
    decoder[0x81] = add_r(WordRegister::C);
    decoder[0x82] = add_r(WordRegister::D);
    decoder[0x83] = add_r(WordRegister::E);
    decoder[0x84] = add_r(WordRegister::H);
    decoder[0x85] = add_r(WordRegister::L);
    decoder[0x86] = add_ptr_r(RegisterPointer::HL);
    decoder[0x87] = add_r(WordRegister::A);
    decoder[0x90] = sub_r(WordRegister::B);
    decoder[0x91] = sub_r(WordRegister::C);
    decoder[0x92] = sub_r(WordRegister::D);
    decoder[0x93] = sub_r(WordRegister::E);
    decoder[0x94] = sub_r(WordRegister::H);
    decoder[0x95] = sub_r(WordRegister::L);
    decoder[0x96] = sub_ptr_r(RegisterPointer::HL);
    decoder[0x97] = sub_r(WordRegister::A);
    decoder[0xA8] = xor_r(WordRegister::B);
    decoder[0xA9] = xor_r(WordRegister::C);
    decoder[0xAA] = xor_r(WordRegister::D);
    decoder[0xAB] = xor_r(WordRegister::E);
    decoder[0xAC] = xor_r(WordRegister::H);
    decoder[0xAD] = xor_r(WordRegister::L);
    decoder[0xD0] = Box::new(ConditionalReturn::ret_nc());
    decoder[0xEE] = xor_ptr_r(RegisterPointer::HL);
    decoder[0xAF] = xor_r(WordRegister::A);
    decoder[0xC3] = jmp_nn();
    decoder[0xCD] = Box::new(UnconditionalCall::new());
    decoder[0xE0] = Box::new(Load::<Word, HighMemoryPointer, WordRegister>::ldh_ptr_a());
    decoder[0xE2] = ld_ptr_r_from_r(RegisterPointer::C, WordRegister::A);
    decoder[0xEA] = ld_ptr_nn_from_r(WordRegister::A);
    decoder[0xAE] = xor_n();
    decoder[0xF0] = Box::new(Load::<Word, HighMemoryPointer, WordRegister>::ldh_a_ptr());
    decoder[0xF2] = ld_r_from_ptr_r(WordRegister::A, RegisterPointer::C);
    decoder[0xF3] = di();
    decoder[0xF9] = ld_rr_from_rr(DoubleRegister::SP, DoubleRegister::HL);
    decoder[0xFA] = ld_r_from_ptr_nn(WordRegister::A);

    decoder
}

struct ArithmeticOperationOnRegisterA<S: RightOperand<Word>> {
    source: S,
    operation: fn(Word, Word) -> ArithmeticResult,
    size: Size,
    cycles: Cycle
}

impl<S: RightOperand<Word>> Opcode for ArithmeticOperationOnRegisterA<S> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let b = self.source.resolve(cpu);
        let a = cpu.get_a_register();
        let r = (self.operation)(a, b);
        cpu.set_register_a(r.result);
        cpu.set_flags(r.flags);
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}

struct IncDec<D: LeftOperand<Word> + RightOperand<Word>> {
    destination: D,
    operation: fn(Word, Word) -> ArithmeticResult,
    size: Size,
    cycles: Cycle
}

impl<D: LeftOperand<Word> + RightOperand<Word>> Opcode for IncDec<D> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let x = self.destination.resolve(cpu);
        let r = (self.operation)(x, 1);
        self.destination.alter(cpu, r.result);
        //unfortunately this instruction doesn't set the carry flag
        cpu.set_zero_flag(r.flags.zero_flag());
        cpu.set_add_sub_flag(r.flags.add_sub_flag());
        cpu.set_half_carry_flag(r.flags.half_carry_flag());
    }

    fn size(&self) -> Size {
        self.size
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        self.cycles
    }
}

enum HlOp {
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

pub struct ArrayBasedMemory {
    words: [Word; 0xFFFF + 1]
}

impl ArrayBasedMemory {
    fn word_at(&self, address: Address) -> Word {
        self.words[address as usize]
    }

    fn double_at(&self, address: Address) -> Double {
        let high = self.word_at(address + 1);
        let low = self.word_at(address);
        set_low_word(set_high_word(0, high), low)
    }

    fn map(&mut self, p: &Program) {
        for i in 0..p.content.len() {
            self.words[i] = p.content[i];
        }
    }

    fn set_word_at(&mut self, address: Address, word: Word) {
        self.words[address as usize] = word;
    }

    fn set_double_at(&mut self, address: Address, double: Double) {
        let i = address as usize;
        self.words[i] = low_word(double);
        self.words[i + 1] = high_word(double);
    }
}

struct Program {
    name: &'static str,
    content: Vec<Word>
}

struct ComputerUnit {
    registers: Registers,
    memory: ArrayBasedMemory,
    cycles: Cycle,
    ime: bool
}

impl ComputerUnit {
    fn inc_pc(&mut self, length: Double) {
        self.registers.pc = self.registers.pc + length;
    }

    fn run_1_instruction(&mut self, decoder: &Decoder) {
        let word = self.memory.word_at(self.registers.pc);
        let ref opcode = decoder[word];
        opcode.exec(self);
        self.inc_pc(opcode.size());
        self.cycles = self.cycles.wrapping_add(opcode.cycles(self));
    }

    fn load(&mut self, program: &Program) {
        self.memory.map(&program)
    }

    fn get_a_register(&self) -> Word {
        self.registers.a()
    }

    fn get_b_register(&self) -> Word {
        self.registers.b()
    }

    fn get_c_register(&self) -> Word {
        self.registers.c()
    }

    fn get_d_register(&self) -> Word {
        self.registers.d()
    }

    fn get_e_register(&self) -> Word {
        self.registers.e()
    }

    fn get_h_register(&self) -> Word {
        self.registers.h()
    }

    fn get_l_register(&self) -> Word {
        self.registers.l()
    }

    fn get_pc_register(&self) -> Double {
        self.registers.pc
    }

    fn get_sp_register(&self) -> Double {
        self.registers.sp
    }

    fn get_hl_register(&self) -> Double {
        self.registers.hl
    }

    fn get_bc_register(&self) -> Double {
        self.registers.bc
    }

    fn get_de_register(&self) -> Double {
        self.registers.de
    }

    fn set_register_a(&mut self, word: Word) {
        self.registers.af = set_high_word(self.registers.af, word)
    }

    fn set_register_b(&mut self, word: Word) {
        self.registers.bc = set_high_word(self.registers.bc, word)
    }

    fn set_register_c(&mut self, word: Word) {
        self.registers.bc = set_low_word(self.registers.bc, word)
    }

    fn set_register_bc(&mut self, double: Double) {
        self.registers.bc = double
    }

    fn set_register_d(&mut self, word: Word) {
        self.registers.de = set_high_word(self.registers.de, word)
    }

    fn set_register_e(&mut self, word: Word) {
        self.registers.de = set_low_word(self.registers.de, word)
    }

    fn set_register_de(&mut self, double: Double) {
        self.registers.de = double
    }

    fn set_register_h(&mut self, word: Word) {
        self.registers.hl = set_high_word(self.registers.hl, word)
    }

    fn set_register_l(&mut self, word: Word) {
        self.registers.hl = set_low_word(self.registers.hl, word)
    }

    fn set_register_hl(&mut self, double: Double) {
        self.registers.hl = double
    }

    fn set_register_sp(&mut self, double: Double) {
        self.registers.sp = double
    }

    fn set_register_pc(&mut self, double: Double) {
        self.registers.pc = double
    }

    fn word_at(&self, address: Address) -> Word {
        self.memory.word_at(address)
    }

    fn double_at(&self, address: Address) -> Double {
        self.memory.double_at(address)
    }

    fn set_word_at(&mut self, address: Address, word: Word) {
        self.memory.set_word_at(address, word);
    }

    fn set_double_at(&mut self, address: Address, double: Double) {
        self.memory.set_double_at(address, double);
    }

    fn interrupt_master(&self) -> bool {
        self.ime
    }

    fn disable_interrupt_master(&mut self) {
        self.ime = false
    }

    fn zero_flag(&self) -> bool {
        self.registers.zero_flag()
    }

    fn carry_flag(&self) -> bool {
        self.registers.carry_flag()
    }

    fn half_carry_flag(&self) -> bool {
        self.registers.half_carry_flag()
    }

    fn add_sub_flag(&self) -> bool {
        self.registers.add_sub_flag()
    }

    fn set_zero_flag(&mut self, flag_value: bool) {
        self.registers.set_zero_flag(flag_value)
    }

    fn set_carry_flag(&mut self, flag_value: bool) {
        self.registers.set_carry_flag(flag_value)
    }

    fn set_half_carry_flag(&mut self, flag_value: bool) {
        self.registers.set_half_carry_flag(flag_value)
    }

    fn set_add_sub_flag(&mut self, flag_value: bool) {
        self.registers.set_add_sub_flag(flag_value)
    }

    fn set_flags(&mut self, flags: FlagRegister) {
        self.registers.flags = flags;
    }

    fn push(&mut self, double: Double) {
        let original_sp = self.get_sp_register();
        self.set_register_sp(original_sp - 2);
        let sp = self.get_sp_register();
        self.set_double_at(sp, double);
    }

    fn pop(&mut self) -> Double {
        let sp = self.get_sp_register();
        let value = self.double_at(sp);
        self.set_register_pc(sp + 2);
        value
    }
}

fn new_cpu() -> ComputerUnit {
    ComputerUnit {
        registers: Registers::new(),
        memory: ArrayBasedMemory {
            words: [0xAA; 0xFFFF + 1]
        },
        ime: true,
        cycles: 0xA0 // this is some random value
    }
}

pub struct MemoryProgramLoader {}

impl MemoryProgramLoader {
    fn load(&self, input: Vec<Word>) -> Program {
        Program {
            name: "memory",
            content: input
        }
    }
}

struct ArithmeticLogicalUnit {}

#[derive(Debug, PartialEq, Eq)]
struct ArithmeticResult {
    result: Word,
    flags: FlagRegister
}

impl ArithmeticLogicalUnit {
    fn add(a: Word, b: Word) -> ArithmeticResult {
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

    fn sub(a: Word, b: Word) -> ArithmeticResult {
        let two_complement = (!b).wrapping_add(1);
        let mut add = ArithmeticLogicalUnit::add(a, two_complement);
        add.flags.n = true;
        add
    }

    fn has_carry(a: Word, b: Word) -> bool {
        let overflowing_result: u16 = a as u16 + b as u16;
        (overflowing_result & 0x0100) != 0
    }

    fn has_half_carry(a: Word, b: Word) -> bool {
        fn low_nibble(a: Word) -> u8 {
            a & 0xF
        }

        let nibble = low_nibble(a) + low_nibble(b);
        (nibble & 0x10) != 0
    }
}

#[test]
fn should_add() {
    assert_eq!(ArithmeticLogicalUnit::add(1, 1), ArithmeticResult {
        result: 0b10,
        flags: FlagRegister {
            cy: false,
            h: false,
            zf: false,
            n: false
        }
    });
    assert_eq!(ArithmeticLogicalUnit::add(0b1000, 0b1000), ArithmeticResult {
        result: 0b10000,
        flags: FlagRegister {
            cy: false,
            h: true,
            zf: false,
            n: false
        }
    });
    assert_eq!(ArithmeticLogicalUnit::add(0b1000_0000, 0b1000_0000), ArithmeticResult {
        result: 0,
        flags: FlagRegister {
            cy: true,
            h: false,
            zf: true,
            n: false
        }
    });
    assert_eq!(ArithmeticLogicalUnit::add(0b1111_1000, 0b1000), ArithmeticResult {
        result: 0,
        flags: FlagRegister {
            cy: true,
            h: true,
            zf: true,
            n: false
        }
    });

    assert_eq!(ArithmeticLogicalUnit::add(0b01, 0b1111_1110).result, 0xFF);

    assert!(ArithmeticLogicalUnit::add(0xAA, 0xAA).flags.cy);
}

#[test]
fn should_sub() {
    assert_eq!(ArithmeticLogicalUnit::sub(1, 1), ArithmeticResult {
        result: 0,
        flags: FlagRegister {
            cy: true,
            h: true,
            zf: true,
            n: true
        }
    });
    assert_eq!(ArithmeticLogicalUnit::sub(0b01, 0b10).result, ArithmeticLogicalUnit::add(0b01, 0b1111_1110).result);
    assert_eq!(ArithmeticLogicalUnit::sub(0, 1), ArithmeticResult {
        result: 0xFF,
        flags: FlagRegister {
            cy: false,
            h: false,
            zf: false,
            n: true
        }
    });
}

#[test]
fn should_load_program() {
    let mut cpu = new_cpu();

    let program_loader = MemoryProgramLoader {};
    let program = program_loader.load(vec![0x06, 0xBA]); // LD B, 0xBA

    cpu.load(&program);
    cpu.run_1_instruction(&build_decoder());

    assert_eq!(cpu.get_b_register(), 0xBA);
    assert_eq!(cpu.get_pc_register(), 0x02);
}

#[test]
fn should_implement_every_ld_r_w_instructions() {
    trait UseCaseTrait {
        fn program(&self) -> &Program;
        fn assert(&self, ComputerUnit);
    }

    struct UseCase<ASSERTIONS: Fn(ComputerUnit, String) -> ()> {
        program: Program,
        assertions: ASSERTIONS
    }

    impl<ASSERTIONS: Fn(ComputerUnit, String) -> ()> UseCaseTrait for UseCase<ASSERTIONS> {
        fn assert(&self, cpu: ComputerUnit) {
            (self.assertions)(cpu, self.program.name.to_string());
        }
        fn program(&self) -> &Program {
            &self.program
        }
    }

    let cases: Vec<Box<UseCaseTrait>> = vec!(
        Box::new(UseCase {
            program: Program {
                name: "LD A, 0x60",
                content: vec![0x3E, 0x60]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_a_register(), 0x60, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD B, 0x60",
                content: vec![0x06, 0x60]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_b_register(), 0x60, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD C, 0xE0",
                content: vec![0x0E, 0xE0]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_c_register(), 0xE0, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD D, 0x61",
                content: vec![0x16, 0x61]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_d_register(), 0x61, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD E, 0xE1",
                content: vec![0x1E, 0xE1]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_e_register(), 0xE1, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD H, 0x62",
                content: vec![0x26, 0x62]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_h_register(), 0x62, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD L, 0xE2",
                content: vec![0x2E, 0xE2]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_l_register(), 0xE2, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD BC, 0xABCD",
                content: vec![0x01, 0xCD, 0xAB]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_bc_register(), 0xABCD, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 172, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD DE, 0xABCD",
                content: vec![0x11, 0xCD, 0xAB]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_de_register(), 0xABCD, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 172, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD HL, 0xABCD",
                content: vec![0x21, 0xCD, 0xAB]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_hl_register(), 0xABCD, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 172, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD SP, 0xABCD",
                content: vec![0x31, 0xCD, 0xAB]
            },
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_sp_register(), 0xABCD, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 172, "bad cycles count after {}", msg);
            }
        }),
    );

    for case in cases {
        let mut cpu = new_cpu();
        cpu.load(&case.program());
        cpu.run_1_instruction(&build_decoder());
        case.assert(cpu);
        // (case.assertions)(cpu, case.program.name.to_string());
    }
}

#[test]
fn should_implement_ld_b_a_instructions() {
    let pg = Program {
        name: "\nLD B, 0xBB\nLD A, B\n",
        content: vec![0x06, 0xBB, 0x78]
    };
    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.get_b_register(), 0xBB, "bad right register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.get_a_register(), 0xBB, "bad left register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 172, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_c_prt_hl_instructions() {
    let pg = Program {
        name: "LD C,(HL)",
        content: vec![0x4E]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.hl = 0xABCD;
    cpu.memory.words[0xABCD] = 0xEF;
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.get_c_register(), 0xEF, "bad register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_a_prt_bc_instructions() {
    let pg = Program {
        name: "LD A,(BC)",
        content: vec![0x0A]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.bc = 0xABCD;
    cpu.memory.words[0xABCD] = 0xEF;
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.get_a_register(), 0xEF, "bad register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}


#[test]
fn should_implement_ld_a_prt_de_instructions() {
    let pg = Program {
        name: "LD A,(DE)",
        content: vec![0x1A]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.de = 0xABCD;
    cpu.memory.words[0xABCD] = 0xEF;
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.get_a_register(), 0xEF, "bad register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_prt_hl_d_instruction() {
    let pg = Program {
        name: "LD (HL),D",
        content: vec![0x72]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.hl = 0xABCD;
    cpu.registers.de = 0xEF00;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.word_at(cpu.get_hl_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_prt_c_a_instruction() {
    let pg = Program {
        name: "LD (C), A",
        content: vec![0xE2]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.set_register_a(0xEF);
    cpu.set_register_c(0xCD);

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.word_at(0xFFCD), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_a_ptr_c_instruction() {
    let pg = Program {
        name: "LD A, (C)",
        content: vec![0xF2]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.set_register_c(0xCD);
    cpu.set_word_at(0xFFCD, 0xEF);

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.get_a_register(), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_a_hli_instruction() {
    let pg = Program {
        name: "LD A, (HLI)",
        content: vec![0x2A]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.set_register_hl(0xABCD);
    cpu.set_word_at(0xABCD, 0xEF);

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.get_a_register(), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_prt_nn_a_instruction() {
    let pg = Program {
        name: "LD (0xABCD),A",
        content: vec![0xEA, 0xCD, 0xAB] // little endian
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.af = 0xEF00;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.word_at(0xABCD), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 176, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_prt_hl_a_instruction() {
    let pg = Program {
        name: "LD (HL),A",
        content: vec![0x77]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.hl = 0xABCD;
    cpu.registers.af = 0xEF00;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.word_at(cpu.get_hl_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_prt_bc_a_instruction() {
    let pg = Program {
        name: "LD (BC),A",
        content: vec![0x02]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.bc = 0xABCD;
    cpu.registers.af = 0xEF00;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.word_at(cpu.get_bc_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_prt_de_a_instruction() {
    let pg = Program {
        name: "LD (DE),A",
        content: vec![0x12]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.de = 0xABCD;
    cpu.registers.af = 0xEF00;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.word_at(cpu.get_de_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_prt_hl_n_instruction() {
    let pg = Program {
        name: "LD (HL),0x66",
        content: vec![0x36, 0x66]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.hl = 0xABCD;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.word_at(cpu.get_hl_register()), 0x66, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 172, "bad cycles count after {}", msg);
}


#[test]
fn should_implement_ld_a_prt_nn_instruction() {
    let pg = Program {
        name: "LD A,(0xABCD)",
        content: vec![0xFA, 0xCD, 0xAB] // little endian
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.memory.words[0xABCD] = 0x66;
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.get_a_register(), 0x66, "bad register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 176, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_sp_hl_instruction() {
    let pg = Program {
        name: "LD SP,HL",
        content: vec![0xF9]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.set_register_hl(0xABCD);
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.get_sp_register(), 0xABCD, "bad register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_ptr_nn_sp_instruction() {
    let pg = Program {
        name: "LD (0xABCD),SP",
        content: vec![0x08, 0xCD, 0xAB]
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.set_register_sp(0x1234);
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.double_at(0xABCD), 0x1234, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 180, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_dec_instruction() {
    let pg = Program {
        name: "DEC B",
        content: vec![0x05]
    };

    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.set_register_b(0);
    assert_eq!(cpu.cycles, 160);

    cpu.run_1_instruction(&build_decoder());
    assert_eq!(cpu.get_b_register(), 0xFF);
    assert_eq!(cpu.get_pc_register(), 1);
    assert!(!cpu.zero_flag());
    assert!(!cpu.carry_flag());
    assert_eq!(cpu.cycles, 164);
}

#[test]
fn should_run_bios() {
    use std::io::prelude::*;
    use std::fs::File;
    let mut f = File::open("roms/gunsriders.gb").unwrap();
    let mut s = vec!();
    f.read_to_end(&mut s).unwrap();
    let pg = Program {
        name: "Guns Rider",
        content: s
    };

    let msg = pg.name;
    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.pc = 0x100;
    let decoder = build_decoder();

    cpu.run_1_instruction(&decoder); // NOP
    assert_eq!(cpu.get_pc_register(), 0x101, "bad pc after {}", msg);

    cpu.run_1_instruction(&decoder); // JP 0x0150
    assert_eq!(cpu.get_pc_register(), 0x150, "bad pc after {}", msg);

    cpu.run_1_instruction(&decoder); // DI
    assert!(!cpu.interrupt_master(), "the interrupt master flag should not be set");
    assert_eq!(cpu.get_pc_register(), 0x151, "bad pc after {}", msg);

    cpu.run_1_instruction(&decoder); // LD D,A (stupid!)
    assert_eq!(cpu.get_pc_register(), 0x152, "bad pc after {}", msg);

    cpu.run_1_instruction(&decoder); // XOR A (A = 0)
    assert_eq!(cpu.get_pc_register(), 0x153, "bad pc after {}", msg);
    assert_eq!(cpu.get_a_register(), 0x00, "bad A register value after {}", msg);
    assert!(cpu.zero_flag(), "zero flag should be set after {}", msg);
    assert!(!cpu.carry_flag(), "carry flag should not be set after {}", msg);
    assert!(!cpu.half_carry_flag(), "half carry flag should not be set after {}", msg);
    assert!(!cpu.add_sub_flag(), "add/sub flag should not be set after {}", msg);

    cpu.run_1_instruction(&decoder); // LD SP, 0xE000
    assert_eq!(cpu.get_pc_register(), 0x156, "bad pc after {}", msg);
    assert_eq!(cpu.get_sp_register(), 0xE000, "bad SP register value after {}", msg);

    cpu.run_1_instruction(&decoder); // LD HL, 0xDFFF
    assert_eq!(cpu.get_pc_register(), 345, "bad pc after {}", msg);
    assert_eq!(cpu.get_hl_register(), 0xDFFF, "bad HL register value after {}", msg);

    cpu.run_1_instruction(&decoder); // LD C, 0x20
    assert_eq!(cpu.get_pc_register(), 347, "bad pc after {}", msg);
    assert_eq!(cpu.get_c_register(), 0x20, "bad C register value after {}", msg);

    cpu.run_1_instruction(&decoder); // LD B, 0
    assert_eq!(cpu.get_pc_register(), 349, "bad pc after {}", msg);
    assert_eq!(cpu.get_b_register(), 0, "bad C register value after {}", msg);

    cpu.run_1_instruction(&decoder); // LD (HL-), A
    assert_eq!(cpu.get_pc_register(), 350, "bad pc after {}", msg);
    assert_eq!(cpu.get_hl_register(), 0xDFFE, "bad register value after {}", msg);
    assert_eq!(cpu.word_at(0xDFFF), 0, "bad memory value after {}", msg);

    cpu.run_1_instruction(&decoder); // DEC B
    assert_eq!(cpu.get_pc_register(), 351, "bad pc after {}", msg);
    assert_eq!(cpu.get_b_register(), 0xFF, "bad register value after {}", msg);
    assert!(!cpu.zero_flag());
    assert!(cpu.add_sub_flag());
    assert!(!cpu.carry_flag());
    assert!(!cpu.half_carry_flag());

    cpu.run_1_instruction(&decoder); // JR nz,0xFC
    assert_eq!(cpu.get_pc_register(), 349, "bad pc after {}", msg);
    assert_eq!(cpu.get_b_register(), 0xFF, "bad register value after {}", msg);

    while cpu.get_pc_register() != 0x017A {
        cpu.run_1_instruction(&decoder);
    }

    cpu.run_1_instruction(&decoder); // CALL 0x56D4
    assert_eq!(cpu.get_pc_register(), 0x56D4);
    assert_eq!(cpu.get_sp_register(), 0xE000 - 2, "SP is decremented");
    assert_eq!(cpu.double_at(cpu.get_sp_register()), 0x017A, "the return address is set on stack");

    cpu.run_1_instruction(&decoder); // LD A, (0xFF00 + 40) [LCD Control]
    assert_eq!(cpu.get_pc_register(), 0x56D6);
    assert_eq!(cpu.get_a_register(), 0xAA);

    cpu.run_1_instruction(&decoder); // ADD A
    assert_eq!(cpu.get_pc_register(), 0x56D7);
    assert!(cpu.carry_flag(), "0xAA + 0xAA produces a carry");

    cpu.run_1_instruction(&decoder); // RET NC
    assert_eq!(cpu.get_pc_register(), 0x56D8, "Didn't return because there is a carry")

    
}


