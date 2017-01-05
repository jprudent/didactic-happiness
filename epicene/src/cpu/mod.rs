mod opcodes;
mod alu;

use std::ops::IndexMut;
use std::ops::Index;
use self::operands::{WordRegister, DoubleRegister, RegisterPointer, HlOp};

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


pub struct Registers {
    af: Double,
    bc: Double,
    de: Double,
    hl: Double,
    sp: Double,
    pc: Double,
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
        }
    }

    fn a(&self) -> Word {
        high_word(self.af)
    }

    fn set_af(&mut self, double: Double) {
        self.af = double;
    }

    fn b(&self) -> Word {
        high_word(self.bc)
    }

    fn c(&self) -> Word {
        low_word(self.bc)
    }

    fn set_bc(&mut self, double: Double) {
        self.bc = double;
    }

    fn d(&self) -> Word {
        high_word(self.de)
    }

    fn e(&self) -> Word {
        low_word(self.de)
    }

    fn set_de(&mut self, double: Double) {
        self.de = double;
    }

    fn h(&self) -> Word {
        high_word(self.hl)
    }

    fn l(&self) -> Word {
        low_word(self.hl)
    }

    fn set_hl(&mut self, double: Double) {
        self.hl = double;
    }

    fn zero_flag(&self) -> bool {
        self.af & 0b0000_0000_1000_0000 != 0
    }

    fn carry_flag(&self) -> bool {
        self.af & 0b0000_0000_0001_0000 != 0
    }

    fn half_carry_flag(&self) -> bool {
        self.af & 0b0000_0000_0010_0000 != 0
    }

    fn add_sub_flag(&self) -> bool {
        self.af & 0b0000_0000_0100_0000 != 0
    }

    fn set_zero_flag(&mut self, flag_value: bool) {
        if flag_value {
            self.af = self.af | 0b0000_0000_1000_0000
        } else {
            self.af = self.af & 0b1111_1111_0111_1111
        }
    }

    fn set_carry_flag(&mut self, flag_value: bool) {
        if flag_value {
            self.af = self.af | 0b0000_0000_0001_0000
        } else {
            self.af = self.af & 0b1111_1111_1110_1111
        }
    }

    fn set_half_carry_flag(&mut self, flag_value: bool) {
        if flag_value {
            self.af = self.af | 0b0000_0000_0010_0000
        } else {
            self.af = self.af & 0b1111_1111_1101_1111
        }
    }

    fn set_add_sub_flag(&mut self, flag_value: bool) {
        if flag_value {
            self.af = self.af | 0b0000_0000_0100_0000
        } else {
            self.af = self.af & 0b1111_1111_1011_1111
        }
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
        pc: 0x8765
    };
    assert_eq!(regs.af, 0xAAFF);
    assert_eq!(regs.b(), 0xBB);
    assert_eq!(regs.c(), 0xCC);
}


// todo move in opcodes
pub mod operands;


pub trait Opcode {
    fn exec(&self, cpu: &mut ComputerUnit);
    fn size(&self) -> Size;
    fn cycles(&self, &ComputerUnit) -> Cycle;
}


struct Decoder(Vec<Box<Opcode>>);

impl Decoder {
    fn new_basic() -> Decoder {
        let mut decoder = Decoder(vec!());

        //todo temp loop for growing the vec
        for i in 0..256 {
            use self::opcodes::not_implemented::not_implemented;
            decoder.push(not_implemented(i as Word))
        }

        use self::opcodes::load::*;
        use self::opcodes::compare::*;
        use self::opcodes::pop::*;
        use self::opcodes::push::*;
        use self::opcodes::ret::*;
        use self::opcodes::ret_cond::*;
        use self::opcodes::nop::*;
        use self::opcodes::enable_interrupts::*;
        use self::opcodes::disable_interrupts::*;
        use self::opcodes::unconditional_jump::*;
        use self::opcodes::call::*;
        use self::opcodes::inc_dec::*;
        use self::opcodes::inc_dec_16::*;
        use self::opcodes::maths::*;
        use self::opcodes::jr::*;
        use self::opcodes::xor::*;
        use self::opcodes::prefix_cb::*;
        decoder[0x00] = nop();
        decoder[0x01] = ld_rr_from_ww(DoubleRegister::BC);
        decoder[0x02] = ld_ptr_r_from_r(RegisterPointer::BC, WordRegister::A);
        decoder[0x03] = inc_bc();
        decoder[0x04] = inc_r(WordRegister::B);
        decoder[0x05] = dec_r(WordRegister::B);
        decoder[0x06] = ld_r_from_w(WordRegister::B);
        decoder[0x07] = rlc_a();
        decoder[0x08] = ld_ptr_nn_from_rr(DoubleRegister::SP);
        decoder[0x0A] = ld_r_from_ptr_r(WordRegister::A, RegisterPointer::BC);
        decoder[0x0C] = inc_r(WordRegister::C);
        decoder[0x0D] = dec_r(WordRegister::C);
        decoder[0x0E] = ld_r_from_w(WordRegister::C);
        decoder[0x0F] = rrc_a();
        decoder[0x11] = ld_rr_from_ww(DoubleRegister::DE);
        decoder[0x12] = ld_ptr_r_from_r(RegisterPointer::DE, WordRegister::A);
        decoder[0x13] = inc_de();
        decoder[0x14] = inc_r(WordRegister::D);
        decoder[0x15] = dec_r(WordRegister::D);
        decoder[0x16] = ld_r_from_w(WordRegister::D);
        decoder[0x17] = rl_a();
        decoder[0x18] = jr_w();
        decoder[0x1A] = ld_r_from_ptr_r(WordRegister::A, RegisterPointer::DE);
        decoder[0x1C] = inc_r(WordRegister::E);
        decoder[0x1D] = dec_r(WordRegister::E);
        decoder[0x1E] = ld_r_from_w(WordRegister::E);
        decoder[0x1F] = rr_a();
        decoder[0x20] = jr_nz_w();
        decoder[0x21] = ld_rr_from_ww(DoubleRegister::HL);
        decoder[0x22] = ld_ptr_hl_from_a(HlOp::HLI);
        decoder[0x23] = inc_hl();
        decoder[0x24] = inc_r(WordRegister::H);
        decoder[0x25] = dec_r(WordRegister::H);
        decoder[0x26] = ld_r_from_w(WordRegister::H);
        decoder[0x28] = jr_z_w();
        decoder[0x2A] = ld_a_from_ptr_hl(HlOp::HLI);
        decoder[0x2B] = dec_hl();
        decoder[0x2C] = inc_r(WordRegister::L);
        decoder[0x2D] = dec_r(WordRegister::L);
        decoder[0x2E] = ld_r_from_w(WordRegister::L);
        decoder[0x30] = jr_nc_w();
        decoder[0x31] = ld_rr_from_ww(DoubleRegister::SP);
        decoder[0x32] = ld_ptr_hl_from_a(HlOp::HLD);
        decoder[0x33] = inc_sp();
        decoder[0x34] = inc_ptr_r(RegisterPointer::HL);
        decoder[0x35] = dec_ptr_r(RegisterPointer::HL);
        decoder[0x36] = ld_ptr_r_from_w(RegisterPointer::HL);
        decoder[0x38] = jr_c_w();
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
        decoder[0x80] = add_a_r(WordRegister::B);
        decoder[0x81] = add_a_r(WordRegister::C);
        decoder[0x82] = add_a_r(WordRegister::D);
        decoder[0x83] = add_a_r(WordRegister::E);
        decoder[0x84] = add_a_r(WordRegister::H);
        decoder[0x85] = add_a_r(WordRegister::L);
        decoder[0x86] = add_ptr_r(RegisterPointer::HL);
        decoder[0x87] = add_a_r(WordRegister::A);
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
        decoder[0xC9] = ret();
        decoder[0xAE] = xor_n();
        decoder[0xAF] = xor_r(WordRegister::A);
        decoder[0xB0] = or_b();
        decoder[0xB1] = or_c();
        decoder[0xB2] = or_d();
        decoder[0xB3] = or_e();
        decoder[0xB4] = or_h();
        decoder[0xB5] = or_l();
        decoder[0xB6] = or_ptr_hl();
        decoder[0xB7] = or_a();
        decoder[0xC1] = pop_bc();
        decoder[0xC3] = jp_nn();
        decoder[0xC4] = call_nz_a16();
        decoder[0xC5] = push_bc();
        decoder[0xC6] = add_a_d8();
        decoder[0xCB] = prefix_cb();
        decoder[0xCC] = call_z_a16();
        decoder[0xCD] = call_a16();
        decoder[0xD0] = ret_nc();
        decoder[0xD1] = pop_de();
        decoder[0xD4] = call_nc_a16();
        decoder[0xD5] = push_de();
        decoder[0xD6] = sub_d8();
        decoder[0xDC] = call_c_a16();
        decoder[0xE0] = ldh_ptr_a();
        decoder[0xE1] = pop_hl();
        decoder[0xE2] = ld_ptr_r_from_r(RegisterPointer::C, WordRegister::A);
        decoder[0xE5] = push_hl();
        decoder[0xE6] = and_w();
        decoder[0xE8] = add_sp_w();
        decoder[0xE9] = jp_hl();
        decoder[0xEA] = ld_ptr_nn_from_r(WordRegister::A);
        decoder[0xEE] = xor_ptr_r(RegisterPointer::HL);
        decoder[0xF0] = ldh_a_ptr();
        decoder[0xF1] = pop_af();
        decoder[0xF2] = ld_r_from_ptr_r(WordRegister::A, RegisterPointer::C);
        decoder[0xF3] = di();
        decoder[0xF5] = push_af();
        decoder[0xF8] = ld_hl_sp_plus_w();
        decoder[0xF9] = ld_rr_from_rr(DoubleRegister::SP, DoubleRegister::HL);
        decoder[0xFA] = ld_r_from_ptr_nn(WordRegister::A);
        decoder[0xFB] = ei();
        decoder[0xFE] = cp_w();
        decoder[0xFF] = rst_38();
        decoder
    }


    fn push(&mut self, opcode: Box<Opcode>) {
        self.0.push(opcode)
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

pub struct ComputerUnit {
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

    fn get_af_register(&self) -> Double {
        self.registers.af
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
        self.registers.set_bc(double)
    }

    fn set_register_af(&mut self, double: Double) {
        self.registers.set_af(double)
    }

    fn set_register_d(&mut self, word: Word) {
        self.registers.de = set_high_word(self.registers.de, word)
    }

    fn set_register_e(&mut self, word: Word) {
        self.registers.de = set_low_word(self.registers.de, word)
    }

    fn set_register_de(&mut self, double: Double) {
        self.registers.set_de(double)
    }

    fn set_register_h(&mut self, word: Word) {
        self.registers.hl = set_high_word(self.registers.hl, word)
    }

    fn set_register_l(&mut self, word: Word) {
        self.registers.hl = set_low_word(self.registers.hl, word)
    }

    fn set_register_hl(&mut self, double: Double) {
        self.registers.set_hl(double)
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

    fn enable_interrupt_master(&mut self) {
        self.ime = true
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

    fn push(&mut self, double: Double) {
        let original_sp = self.get_sp_register();
        self.set_register_sp(original_sp - 2);
        let sp = self.get_sp_register();
        self.set_double_at(sp, double);
    }

    fn pop(&mut self) -> Double {
        let sp = self.get_sp_register();
        let value = self.double_at(sp);
        self.set_register_sp(sp + 2);
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



#[test]
fn should_load_program() {
    let mut cpu = new_cpu();

    let program_loader = MemoryProgramLoader {};
    let program = program_loader.load(vec![0x06, 0xBA]); // LD B, 0xBA

    cpu.load(&program);
    cpu.run_1_instruction(&Decoder::new_basic());

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
        cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
    assert_eq!(cpu.get_b_register(), 0xBB, "bad right register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
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

    cpu.run_1_instruction(&Decoder::new_basic());
    assert_eq!(cpu.get_b_register(), 0xFF);
    assert_eq!(cpu.get_pc_register(), 1);
    assert!(!cpu.zero_flag());
    assert!(cpu.add_sub_flag());
    assert_eq!(cpu.cycles, 164);
}

#[test]
fn should_run_gunsriders() {
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
    let decoder = &Decoder::new_basic();

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
    assert!(cpu.half_carry_flag());

    cpu.run_1_instruction(&decoder); // JR nz,0xFC
    assert_eq!(cpu.get_pc_register(), 349, "jump is taken");
    assert_eq!(cpu.get_b_register(), 0xFF, "bad register value after {}", msg);

    while cpu.get_pc_register() != 0x017A {
        cpu.run_1_instruction(&decoder);
    }

    cpu.run_1_instruction(&decoder); // CALL 0x56D4
    assert_eq!(cpu.get_pc_register(), 0x56D4);
    assert_eq!(cpu.get_sp_register(), 0xE000 - 2, "SP is decremented");
    assert_eq!(cpu.double_at(cpu.get_sp_register()), 0x017D, "the return address is set on stack");

    cpu.run_1_instruction(&decoder); // LD A, (0xFF00 + 40) [LCD Control]
    assert_eq!(cpu.get_pc_register(), 0x56D6);
    assert_eq!(cpu.get_a_register(), 0xAA);

    cpu.run_1_instruction(&decoder); // ADD A
    assert_eq!(cpu.get_pc_register(), 0x56D7);
    assert!(cpu.carry_flag(), "0xAA + 0xAA produces a carry");

    cpu.run_1_instruction(&decoder); // RET NC
    assert_eq!(cpu.get_pc_register(), 0x56D8, "Didn't return because there is a carry");

    cpu.set_word_at(0xFF44, 0x90);
    cpu.run_1_instruction(&decoder); // LD A, (0xFF00 + 0x44) [LY]
    assert_eq!(cpu.get_pc_register(), 0x56DA);
    assert_eq!(cpu.get_a_register(), 0x90);


    cpu.run_1_instruction(&decoder); // CP A,0x92
    assert_eq!(cpu.get_pc_register(), 0x56DC);
    assert!(!cpu.zero_flag());
    assert!(cpu.carry_flag());
    assert!(cpu.half_carry_flag());
    assert!(cpu.add_sub_flag());

    cpu.run_1_instruction(&decoder); // JR NC, 0xFA
    assert_eq!(cpu.get_pc_register(), 0x56DE, "Jump is not taken");

    cpu.set_word_at(0xFF44, 0x91);
    cpu.run_1_instruction(&decoder); // LD A, (0xFF00 + 0x44) [LY]
    assert_eq!(cpu.get_pc_register(), 0x56E0);
    assert_eq!(cpu.get_a_register(), 0x91);

    cpu.run_1_instruction(&decoder); // CP A,0x91
    assert_eq!(cpu.get_pc_register(), 0x56E2);
    assert!(cpu.zero_flag());
    assert!(!cpu.carry_flag());
    assert!(!cpu.half_carry_flag());
    assert!(cpu.add_sub_flag());

    cpu.run_1_instruction(&decoder); // JR C,0xFA
    assert_eq!(cpu.get_pc_register(), 0x56E4, "Jump is not taken");

    cpu.set_word_at(0xFF40, 0x91);
    cpu.run_1_instruction(&decoder); // LD A, (0xFF + 0x40)
    assert_eq!(cpu.get_pc_register(), 0x56E6);
    assert_eq!(cpu.get_a_register(), 0x91);

    cpu.run_1_instruction(&decoder); // AND A,0x7F
    assert_eq!(cpu.get_pc_register(), 0x56E8);
    assert_eq!(cpu.get_a_register(), 0x11);
    assert!(!cpu.zero_flag());
    assert!(!cpu.carry_flag());
    assert!(cpu.half_carry_flag());
    assert!(!cpu.add_sub_flag());

    cpu.run_1_instruction(&decoder); // LD (0xFF + 0x40), A
    assert_eq!(cpu.get_pc_register(), 0x56EA);
    assert_eq!(cpu.word_at(0xFF40), 0x11);

    cpu.run_1_instruction(&decoder); // RET
    assert_eq!(cpu.get_pc_register(), 0x017D);

    while cpu.get_pc_register() != 0x56A2 {
        cpu.run_1_instruction(&decoder);
    }
    assert_eq!(cpu.get_hl_register(), 0xC12D);
    assert_eq!(cpu.get_a_register(), 0);
    assert_eq!(cpu.word_at(0xC12D), 0);

    cpu.run_1_instruction(&decoder); // OR (hl)
    assert_eq!(cpu.get_pc_register(), 0x56A3);
    assert_eq!(cpu.get_a_register(), 0);
    assert!(cpu.zero_flag());
    assert!(!cpu.carry_flag());
    assert!(!cpu.half_carry_flag());
    assert!(!cpu.add_sub_flag());

    cpu.run_1_instruction(&decoder); // JR Z,0x03
    assert_eq!(cpu.get_pc_register(), 0x56A8, "Jump is taken");

    assert_eq!(cpu.get_b_register(), 0x56);
    assert_eq!(cpu.get_hl_register(), 0xC12D);
    cpu.run_1_instruction(&decoder); // LD (HL),B
    assert_eq!(cpu.get_pc_register(), 0x56A9);
    assert_eq!(cpu.word_at(0xC12D), 0x56);

    cpu.run_1_instruction(&decoder); // DEC HL
    assert_eq!(cpu.get_pc_register(), 0x56AA);
    assert_eq!(cpu.get_hl_register(), 0xC12C);

    while cpu.get_pc_register() != 0x01CA {
        cpu.run_1_instruction(&decoder);
    }

    cpu.run_1_instruction(&decoder); // EI
    assert_eq!(cpu.get_pc_register(), 0x01CB);
    assert!(cpu.interrupt_master());

    while cpu.get_pc_register() != 0x57BF {
        cpu.run_1_instruction(&decoder);
    }

    assert_eq!(cpu.get_sp_register(), 0xDFFE);
    assert_eq!(cpu.double_at(0xDFFE), 0x01CE);
    cpu.run_1_instruction(&decoder); // POP HL
    assert_eq!(cpu.get_pc_register(), 0x57C0);
    assert_eq!(cpu.get_hl_register(), 0x01CE);
    assert_eq!(cpu.get_sp_register(), 0xE000);

    cpu.run_1_instruction(&decoder); // LD A,(0xC129)
    assert_eq!(cpu.get_pc_register(), 0x57C3);
    assert_eq!(cpu.get_a_register(), 0);

    cpu.run_1_instruction(&decoder); // PUSH AF
    assert_eq!(cpu.get_pc_register(), 0x57C4);
    assert_eq!(cpu.get_sp_register(), 0xDFFE);
    assert_eq!(cpu.double_at(0xDFFE), cpu.registers.af);

    cpu.run_1_instruction(&decoder); // LD E,(HL)
    assert_eq!(cpu.get_pc_register(), 0x57C5);

    cpu.run_1_instruction(&decoder); // INC HL
    assert_eq!(cpu.get_pc_register(), 0x57C6);
    assert_eq!(cpu.get_hl_register(), 0x01CF);

    while cpu.get_pc_register() != 0x57D7 {
        cpu.run_1_instruction(&decoder);
    }

    assert_eq!(cpu.get_hl_register(), 0x318D);
    cpu.run_1_instruction(&decoder);
    assert_eq!(cpu.get_pc_register(), 0x318D);

    cpu.run_1_instruction(&decoder); // PUSH BC
    assert_eq!(cpu.get_pc_register(), 0x318E);
    assert_eq!(cpu.get_sp_register(), 0xDFF8);
    assert_eq!(cpu.double_at(0xDFF8), cpu.registers.bc);

    cpu.run_1_instruction(&decoder); // ADD SP,0xB0
    assert_eq!(cpu.get_pc_register(), 0x3190);
    assert_eq!(cpu.get_sp_register(), 0xDFA8);
    assert!(cpu.carry_flag());
    assert!(!cpu.half_carry_flag());

    cpu.run_1_instruction(&decoder); // ld HL, SP+0x4C
    assert_eq!(cpu.get_hl_register(), 0xDFF4);
    assert!(!cpu.carry_flag());
    assert!(cpu.half_carry_flag());

    while cpu.get_pc_register() != 0x31B0 {
        cpu.run_1_instruction(&decoder);
    }

    //cpu.run_1_instruction(&decoder); // RRCA
    //assert_eq!(cpu.get_pc_register(), 0x31B1);

    //while true {
    //    cpu.run_1_instruction(&decoder);
    //}
}

#[test]
fn should_run_testrom() {
    use std::io::prelude::*;
    use std::fs::File;
    let mut f = File::open("roms/cpu_instrs/cpu_instrs.gb").unwrap();
    let mut s = vec!();
    f.read_to_end(&mut s).unwrap();
    let pg = Program {
        name: "Guns Rider",
        content: s
    };

    let mut cpu = new_cpu();
    cpu.load(&pg);
    cpu.registers.pc = 0x100;
    let decoder = &Decoder::new_basic();

    for i in 0..100000 {
        println!("@{:04X} {:02X} {:02X}", cpu.get_pc_register(), cpu.word_at(cpu.get_pc_register()), cpu.word_at(cpu.get_pc_register() + 1));
        cpu.run_1_instruction(&decoder);
    }

    assert!(false);
}


