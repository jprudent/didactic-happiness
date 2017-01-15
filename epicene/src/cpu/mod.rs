mod opcodes;
mod alu;

use std::ops::IndexMut;
use std::ops::Index;
use self::operands::{WordRegister, DoubleRegister, RegisterPointer, HlOp};
use super::{Address};

// todo duplicated in super::
pub type Word = u8;
type Double = u16;
type Cycle = u8;
type Size = u16;

pub fn high_word(double: Double) -> Word {
    double.wrapping_shr(8) as Word
}

pub fn low_word(double: Double) -> Word {
    (double & 0xFF) as Word
}

pub fn set_high_word(double: Double, word: Word) -> Double {
    (double & 0xFF) | (word as Double).wrapping_shl(8)
}

pub fn set_low_word(double: Double, word: Word) -> Double {
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
        // bits 0-3 are always 0
        self.af = self.af & 0xFFF0
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

// todo that's a pity the opcode doesn't know it's opcode (as a number)
pub trait Opcode {
    fn exec(&self, cpu: &mut ComputerUnit);
    fn size(&self) -> Size;
    fn cycles(&self, &ComputerUnit) -> Cycle;
    fn to_string(&self, cpu: &ComputerUnit) -> String;
}


pub struct Decoder(Vec<Box<Opcode>>);

impl Decoder {
    pub fn new_basic() -> Decoder {
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
        use self::opcodes::ccf::*;
        use self::opcodes::scf::*;
        use self::opcodes::daa::*;
        use self::opcodes::cpl::*;
        use self::opcodes::rlca::*;
        use self::opcodes::rla::*;
        use self::opcodes::rrca::*;
        use self::opcodes::rra::*;
        use self::opcodes::add_hl_rr::*;

        decoder[0x00] = nop();
        decoder[0x01] = ld_rr_from_ww(DoubleRegister::BC);
        decoder[0x02] = ld_ptr_r_from_r(RegisterPointer::BC, WordRegister::A);
        decoder[0x03] = inc_bc();
        decoder[0x04] = inc_r(WordRegister::B);
        decoder[0x05] = dec_r(WordRegister::B);
        decoder[0x06] = ld_b_w();
        decoder[0x07] = rlca();
        decoder[0x08] = ld_ptr_nn_from_rr(DoubleRegister::SP);
        decoder[0x09] = add_hl_bc();
        decoder[0x0A] = ld_r_from_ptr_r(WordRegister::A, RegisterPointer::BC);
        decoder[0x0B] = dec_bc();
        decoder[0x0C] = inc_r(WordRegister::C);
        decoder[0x0D] = dec_r(WordRegister::C);
        decoder[0x0E] = ld_c_w();
        decoder[0x0F] = rrca();
        decoder[0x10] = stop();
        decoder[0x11] = ld_rr_from_ww(DoubleRegister::DE);
        decoder[0x12] = ld_ptr_r_from_r(RegisterPointer::DE, WordRegister::A);
        decoder[0x13] = inc_de();
        decoder[0x14] = inc_r(WordRegister::D);
        decoder[0x15] = dec_r(WordRegister::D);
        decoder[0x16] = ld_d_w();
        decoder[0x17] = rla();
        decoder[0x18] = jr_w();
        decoder[0x19] = add_hl_de();
        decoder[0x1A] = ld_r_from_ptr_r(WordRegister::A, RegisterPointer::DE);
        decoder[0x1B] = dec_de();
        decoder[0x1C] = inc_r(WordRegister::E);
        decoder[0x1D] = dec_r(WordRegister::E);
        decoder[0x1E] = ld_e_w();
        decoder[0x1F] = rra();
        decoder[0x20] = jr_nz_w();
        decoder[0x21] = ld_rr_from_ww(DoubleRegister::HL);
        decoder[0x22] = ld_ptr_hl_from_a(HlOp::HLI);
        decoder[0x23] = inc_hl();
        decoder[0x24] = inc_r(WordRegister::H);
        decoder[0x25] = dec_r(WordRegister::H);
        decoder[0x26] = ld_h_w();
        decoder[0x27] = daa();
        decoder[0x28] = jr_z_w();
        decoder[0x29] = add_hl_hl();
        decoder[0x2A] = ld_a_from_ptr_hl(HlOp::HLI);
        decoder[0x2B] = dec_hl();
        decoder[0x2C] = inc_r(WordRegister::L);
        decoder[0x2D] = dec_r(WordRegister::L);
        decoder[0x2E] = ld_l_w();
        decoder[0x2F] = cpl();
        decoder[0x30] = jr_nc_w();
        decoder[0x31] = ld_rr_from_ww(DoubleRegister::SP);
        decoder[0x32] = ld_ptr_hl_from_a(HlOp::HLD);
        decoder[0x33] = inc_sp();
        decoder[0x34] = inc_ptr_r(RegisterPointer::HL);
        decoder[0x35] = dec_ptr_r(RegisterPointer::HL);
        decoder[0x36] = ld_ptr_r_from_w(RegisterPointer::HL);
        decoder[0x37] = scf();
        decoder[0x38] = jr_c_w();
        decoder[0x39] = add_hl_sp();
        decoder[0x3A] = ld_a_from_ptr_hl(HlOp::HLD);
        decoder[0x3B] = dec_sp();
        decoder[0x3C] = inc_r(WordRegister::A);
        decoder[0x3D] = dec_r(WordRegister::A);
        decoder[0x3E] = ld_a_w();
        decoder[0x3F] = ccf();
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
        decoder[0x88] = adc_a_b();
        decoder[0x89] = adc_a_c();
        decoder[0x8A] = adc_a_d();
        decoder[0x8B] = adc_a_e();
        decoder[0x8C] = adc_a_h();
        decoder[0x8D] = adc_a_l();
        decoder[0x8E] = adc_a_ptr_hl();
        decoder[0x8F] = adc_a_a();
        decoder[0x90] = sub_r(WordRegister::B);
        decoder[0x91] = sub_r(WordRegister::C);
        decoder[0x92] = sub_r(WordRegister::D);
        decoder[0x93] = sub_r(WordRegister::E);
        decoder[0x94] = sub_r(WordRegister::H);
        decoder[0x95] = sub_r(WordRegister::L);
        decoder[0x96] = sub_ptr_r(RegisterPointer::HL);
        decoder[0x97] = sub_r(WordRegister::A);
        decoder[0x98] = sbc_a_b();
        decoder[0x99] = sbc_a_c();
        decoder[0x9A] = sbc_a_d();
        decoder[0x9B] = sbc_a_e();
        decoder[0x9C] = sbc_a_h();
        decoder[0x9D] = sbc_a_l();
        decoder[0x9E] = sbc_a_ptr_hl();
        decoder[0x9F] = sbc_a_a();
        decoder[0xA0] = and_a_b();
        decoder[0xA1] = and_a_c();
        decoder[0xA2] = and_a_d();
        decoder[0xA3] = and_a_e();
        decoder[0xA4] = and_a_h();
        decoder[0xA5] = and_a_l();
        decoder[0xA6] = and_a_ptr_hl();
        decoder[0xA7] = and_a_a();
        decoder[0xA8] = xor_r(WordRegister::B);
        decoder[0xA9] = xor_r(WordRegister::C);
        decoder[0xAA] = xor_r(WordRegister::D);
        decoder[0xAB] = xor_r(WordRegister::E);
        decoder[0xAC] = xor_r(WordRegister::H);
        decoder[0xAD] = xor_r(WordRegister::L);
        decoder[0xAE] = xor_ptr_hl();
        decoder[0xAF] = xor_r(WordRegister::A);
        decoder[0xB0] = or_b();
        decoder[0xB1] = or_c();
        decoder[0xB2] = or_d();
        decoder[0xB3] = or_e();
        decoder[0xB4] = or_h();
        decoder[0xB5] = or_l();
        decoder[0xB6] = or_ptr_hl();
        decoder[0xB7] = or_a();
        decoder[0xB8] = cp_a_b();
        decoder[0xB9] = cp_a_c();
        decoder[0xBA] = cp_a_d();
        decoder[0xBB] = cp_a_e();
        decoder[0xBC] = cp_a_h();
        decoder[0xBD] = cp_a_l();
        decoder[0xBE] = cp_a_ptr_hl();
        decoder[0xBF] = cp_a_a();
        decoder[0xC0] = ret_nz();
        decoder[0xC1] = pop_bc();
        decoder[0xC2] = jp_nz_nn();
        decoder[0xC3] = jp_nn();
        decoder[0xC4] = call_nz_a16();
        decoder[0xC5] = push_bc();
        decoder[0xC6] = add_a_d8();
        decoder[0xC7] = rst_00();
        decoder[0xC8] = ret_z();
        decoder[0xC9] = ret();
        decoder[0xCA] = jp_z_nn();
        decoder[0xCB] = prefix_cb();
        decoder[0xCC] = call_z_a16();
        decoder[0xCD] = call_a16();
        decoder[0xCE] = adc_a_w();
        decoder[0xCF] = rst_08();
        decoder[0xD0] = ret_nc();
        decoder[0xD1] = pop_de();
        decoder[0xD2] = jp_nc_nn();
        decoder[0xD4] = call_nc_a16();
        decoder[0xD5] = push_de();
        decoder[0xD6] = sub_d8();
        decoder[0xD7] = rst_10();
        decoder[0xD8] = ret_c();
        decoder[0xD9] = reti();
        decoder[0xDA] = jp_c_nn();
        decoder[0xDC] = call_c_a16();
        decoder[0xDE] = sbc_a_w();
        decoder[0xDF] = rst_18();
        decoder[0xE0] = ldh_ptr_a();
        decoder[0xE1] = pop_hl();
        decoder[0xE2] = ld_ptr_r_from_r(RegisterPointer::C, WordRegister::A);
        decoder[0xE5] = push_hl();
        decoder[0xE6] = and_w();
        decoder[0xE7] = rst_20();
        decoder[0xE8] = add_sp_w();
        decoder[0xE9] = jp_hl();
        decoder[0xEA] = ld_ptr_nn_from_r(WordRegister::A);
        decoder[0xEE] = xor_n();
        decoder[0xEF] = rst_28();
        decoder[0xF0] = ldh_a_ptr();
        decoder[0xF1] = pop_af();
        decoder[0xF2] = ld_r_from_ptr_r(WordRegister::A, RegisterPointer::C);
        decoder[0xF3] = di();
        decoder[0xF5] = push_af();
        decoder[0xF6] = or_a_w();
        decoder[0xF7] = rst_30();
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

use super::program::Program;
use super::memory::ArrayBasedMemory;
use super::debug::MemoryWriteHook;

pub struct ComputerUnit<'a> {
    registers: Registers,
    memory: ArrayBasedMemory,
    write_memory_hooks: Vec<&'a mut MemoryWriteHook>,
    cycles: Cycle,
    ime: bool,
}

impl<'a> ComputerUnit<'a> {
    pub fn hooked(memory_hooks: Vec<&mut MemoryWriteHook>) -> ComputerUnit {
        ComputerUnit {
            registers: Registers::new(),
            memory: ArrayBasedMemory::new(),
            write_memory_hooks: memory_hooks,
            ime: true,
            cycles: 0xA0, // this is some random value
        }
    }

    pub fn new() -> ComputerUnit<'a> {
        ComputerUnit::hooked(vec!())
    }

    fn inc_pc(&mut self, length: Double) {
        self.registers.pc = self.registers.pc.wrapping_add(length);
    }

    pub fn fetch(&self) -> Word {
        self.memory.word_at(self.registers.pc)
    }

    //TODO internalize the Decoder
    pub fn decode<'d>(&self, fetch: Word, decoder: &'d Decoder) -> &'d Box<Opcode> {
        &decoder[fetch]
    }

    pub fn exec(&mut self, opcode: &Box<Opcode>) {
        opcode.exec(self);
        self.inc_pc(opcode.size());
        self.cycles = self.cycles.wrapping_add(opcode.cycles(self));
    }

    // TODO maybe a constructor would be better
    pub fn load(&mut self, program: &Program) {
        self.memory.map(&program)
    }

    pub fn get_a_register(&self) -> Word {
        self.registers.a()
    }

    pub fn get_b_register(&self) -> Word {
        self.registers.b()
    }

    pub fn get_c_register(&self) -> Word {
        self.registers.c()
    }

    pub fn get_d_register(&self) -> Word {
        self.registers.d()
    }

    pub fn get_e_register(&self) -> Word {
        self.registers.e()
    }

    pub fn get_h_register(&self) -> Word {
        self.registers.h()
    }

    pub fn get_l_register(&self) -> Word {
        self.registers.l()
    }

    pub fn get_pc_register(&self) -> Double {
        self.registers.pc
    }

    pub fn get_sp_register(&self) -> Double {
        self.registers.sp
    }

    pub fn get_hl_register(&self) -> Double {
        self.registers.hl
    }

    pub fn get_af_register(&self) -> Double {
        self.registers.af
    }

    pub fn get_bc_register(&self) -> Double {
        self.registers.bc
    }

    pub fn get_de_register(&self) -> Double {
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

    pub fn set_register_pc(&mut self, double: Double) {
        self.registers.pc = double
    }

    pub fn word_at(&self, address: Address) -> Word {
        self.memory.word_at(address)
    }

    fn double_at(&self, address: Address) -> Double {
        self.memory.double_at(address)
    }

    pub fn set_word_at(&mut self, address: Address, word: Word) {
        for hook in self.write_memory_hooks.iter_mut() {
            hook.apply(address, word);
        }
        self.memory.set_word_at(address, word);
    }

    fn set_double_at(&mut self, address: Address, double: Double) {
        for hook in self.write_memory_hooks.iter_mut() {
            hook.apply(address, high_word(double));
            hook.apply(address + 1, low_word(double));
        }
        self.memory.set_double_at(address, double);
    }

    pub fn interrupt_master(&self) -> bool {
        self.ime
    }

    pub fn disable_interrupt_master(&mut self) {
        self.ime = false
    }

    fn enable_interrupt_master(&mut self) {
        self.ime = true
    }

    pub fn zero_flag(&self) -> bool {
        self.registers.zero_flag()
    }

    pub fn carry_flag(&self) -> bool {
        self.registers.carry_flag()
    }

    pub fn half_carry_flag(&self) -> bool {
        self.registers.half_carry_flag()
    }

    pub fn add_sub_flag(&self) -> bool {
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

    pub fn push(&mut self, double: Double) {
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

mod tests;