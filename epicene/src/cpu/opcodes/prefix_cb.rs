use super::super::{Word, Cycle, Size, Opcode, ComputerUnit, Decoder};
use super::super::operands::{WordRegister};

struct PrefixCb {
    decoder: Decoder
}

pub fn prefix_cb() -> Box<Opcode> {
    Box::new(PrefixCb {
        decoder: build_decoder()
    })
}

fn build_decoder() -> Decoder {
    let mut decoder = Decoder(vec!());

    //todo temp loop for growing the vec
    for i in 0..256 {
        use super::not_implemented::not_implemented;
        decoder.push(not_implemented(i as Word))
    }
    use super::maths::*;
    use super::bit::*;
    use super::res::*;
    use super::set::*;
    decoder[0x00] = rlc_b();
    decoder[0x01] = rlc_c();
    decoder[0x02] = rlc_d();
    decoder[0x03] = rlc_e();
    decoder[0x04] = rlc_h();
    decoder[0x05] = rlc_l();
    decoder[0x06] = rlc_ptr_hl();
    decoder[0x07] = rlc_a();

    decoder[0x08] = rrc_b();
    decoder[0x09] = rrc_c();
    decoder[0x0A] = rrc_d();
    decoder[0x0B] = rrc_e();
    decoder[0x0C] = rrc_h();
    decoder[0x0D] = rrc_l();
    decoder[0x0E] = rrc_ptr_hl();
    decoder[0x0F] = rrc_a();

    decoder[0x10] = rl_b();
    decoder[0x11] = rl_c();
    decoder[0x12] = rl_d();
    decoder[0x13] = rl_e();
    decoder[0x14] = rl_h();
    decoder[0x15] = rl_l();
    decoder[0x16] = rl_ptr_hl();
    decoder[0x17] = rl_a();

    decoder[0x18] = rr_b();
    decoder[0x19] = rr_c();
    decoder[0x1A] = rr_d();
    decoder[0x1B] = rr_e();
    decoder[0x1C] = rr_h();
    decoder[0x1D] = rr_l();
    decoder[0x1E] = rr_ptr_hl();
    decoder[0x1F] = rr_a();

    decoder[0x20] = sla_b();
    decoder[0x21] = sla_c();
    decoder[0x22] = sla_d();
    decoder[0x23] = sla_e();
    decoder[0x24] = sla_h();
    decoder[0x25] = sla_l();
    decoder[0x26] = sla_ptr_hl();
    decoder[0x27] = sla_a();

    decoder[0x28] = sra_b();
    decoder[0x29] = sra_c();
    decoder[0x2A] = sra_d();
    decoder[0x2B] = sra_e();
    decoder[0x2C] = sra_h();
    decoder[0x2D] = sra_l();
    decoder[0x2E] = sra_ptr_hl();
    decoder[0x2F] = sra_a();

    decoder[0x30] = swap_b();
    decoder[0x31] = swap_c();
    decoder[0x32] = swap_d();
    decoder[0x33] = swap_e();
    decoder[0x34] = swap_h();
    decoder[0x35] = swap_l();
    decoder[0x36] = swap_ptr_hl();
    decoder[0x37] = swap_a();

    decoder[0x38] = srl_b();
    decoder[0x39] = srl_c();
    decoder[0x3A] = srl_d();
    decoder[0x3B] = srl_e();
    decoder[0x3C] = srl_h();
    decoder[0x3D] = srl_l();
    decoder[0x3E] = srl_ptr_hl();
    decoder[0x3F] = srl_a();

    decoder[0x40] = bit_n_r(0, WordRegister::B);
    decoder[0x41] = bit_n_r(0, WordRegister::C);
    decoder[0x42] = bit_n_r(0, WordRegister::D);
    decoder[0x43] = bit_n_r(0, WordRegister::E);
    decoder[0x44] = bit_n_r(0, WordRegister::H);
    decoder[0x45] = bit_n_r(0, WordRegister::L);
    decoder[0x46] = bit_n_ptr_hl(0);
    decoder[0x47] = bit_n_r(0, WordRegister::A);

    decoder[0x48] = bit_n_r(1, WordRegister::B);
    decoder[0x49] = bit_n_r(1, WordRegister::C);
    decoder[0x4A] = bit_n_r(1, WordRegister::D);
    decoder[0x4B] = bit_n_r(1, WordRegister::E);
    decoder[0x4C] = bit_n_r(1, WordRegister::H);
    decoder[0x4D] = bit_n_r(1, WordRegister::L);
    decoder[0x4E] = bit_n_ptr_hl(1);
    decoder[0x4F] = bit_n_r(1, WordRegister::A);

    decoder[0x50] = bit_n_r(2, WordRegister::B);
    decoder[0x51] = bit_n_r(2, WordRegister::C);
    decoder[0x52] = bit_n_r(2, WordRegister::D);
    decoder[0x53] = bit_n_r(2, WordRegister::E);
    decoder[0x54] = bit_n_r(2, WordRegister::H);
    decoder[0x55] = bit_n_r(2, WordRegister::L);
    decoder[0x56] = bit_n_ptr_hl(2);
    decoder[0x57] = bit_n_r(2, WordRegister::A);

    decoder[0x58] = bit_n_r(3, WordRegister::B);
    decoder[0x59] = bit_n_r(3, WordRegister::C);
    decoder[0x5A] = bit_n_r(3, WordRegister::D);
    decoder[0x5B] = bit_n_r(3, WordRegister::E);
    decoder[0x5C] = bit_n_r(3, WordRegister::H);
    decoder[0x5D] = bit_n_r(3, WordRegister::L);
    decoder[0x5E] = bit_n_ptr_hl(3);
    decoder[0x5F] = bit_n_r(3, WordRegister::A);

    decoder[0x60] = bit_n_r(4, WordRegister::B);
    decoder[0x61] = bit_n_r(4, WordRegister::C);
    decoder[0x62] = bit_n_r(4, WordRegister::D);
    decoder[0x63] = bit_n_r(4, WordRegister::E);
    decoder[0x64] = bit_n_r(4, WordRegister::H);
    decoder[0x65] = bit_n_r(4, WordRegister::L);
    decoder[0x66] = bit_n_ptr_hl(4);
    decoder[0x67] = bit_n_r(4, WordRegister::A);

    decoder[0x68] = bit_n_r(5, WordRegister::B);
    decoder[0x69] = bit_n_r(5, WordRegister::C);
    decoder[0x6A] = bit_n_r(5, WordRegister::D);
    decoder[0x6B] = bit_n_r(5, WordRegister::E);
    decoder[0x6C] = bit_n_r(5, WordRegister::H);
    decoder[0x6D] = bit_n_r(5, WordRegister::L);
    decoder[0x6E] = bit_n_ptr_hl(5);
    decoder[0x6F] = bit_n_r(5, WordRegister::A);

    decoder[0x70] = bit_n_r(6, WordRegister::B);
    decoder[0x71] = bit_n_r(6, WordRegister::C);
    decoder[0x72] = bit_n_r(6, WordRegister::D);
    decoder[0x73] = bit_n_r(6, WordRegister::E);
    decoder[0x74] = bit_n_r(6, WordRegister::H);
    decoder[0x75] = bit_n_r(6, WordRegister::L);
    decoder[0x76] = bit_n_ptr_hl(6);
    decoder[0x77] = bit_n_r(6, WordRegister::A);

    decoder[0x78] = bit_n_r(7, WordRegister::B);
    decoder[0x79] = bit_n_r(7, WordRegister::C);
    decoder[0x7A] = bit_n_r(7, WordRegister::D);
    decoder[0x7B] = bit_n_r(7, WordRegister::E);
    decoder[0x7C] = bit_n_r(7, WordRegister::H);
    decoder[0x7D] = bit_n_r(7, WordRegister::L);
    decoder[0x7E] = bit_n_ptr_hl(7);
    decoder[0x7F] = bit_n_r(7, WordRegister::A);

    decoder[0x80] = res_n_r(0, WordRegister::B);
    decoder[0x81] = res_n_r(0, WordRegister::C);
    decoder[0x82] = res_n_r(0, WordRegister::D);
    decoder[0x83] = res_n_r(0, WordRegister::E);
    decoder[0x84] = res_n_r(0, WordRegister::H);
    decoder[0x85] = res_n_r(0, WordRegister::L);
    decoder[0x86] = res_n_ptr_hl(0);
    decoder[0x87] = res_n_r(0, WordRegister::A);

    decoder[0x88] = res_n_r(1, WordRegister::B);
    decoder[0x89] = res_n_r(1, WordRegister::C);
    decoder[0x8A] = res_n_r(1, WordRegister::D);
    decoder[0x8B] = res_n_r(1, WordRegister::E);
    decoder[0x8C] = res_n_r(1, WordRegister::H);
    decoder[0x8D] = res_n_r(1, WordRegister::L);
    decoder[0x8E] = res_n_ptr_hl(1);
    decoder[0x8F] = res_n_r(1, WordRegister::A);

    decoder[0x90] = res_n_r(2, WordRegister::B);
    decoder[0x91] = res_n_r(2, WordRegister::C);
    decoder[0x92] = res_n_r(2, WordRegister::D);
    decoder[0x93] = res_n_r(2, WordRegister::E);
    decoder[0x94] = res_n_r(2, WordRegister::H);
    decoder[0x95] = res_n_r(2, WordRegister::L);
    decoder[0x96] = res_n_ptr_hl(2);
    decoder[0x97] = res_n_r(2, WordRegister::A);

    decoder[0x98] = res_n_r(3, WordRegister::B);
    decoder[0x99] = res_n_r(3, WordRegister::C);
    decoder[0x9A] = res_n_r(3, WordRegister::D);
    decoder[0x9B] = res_n_r(3, WordRegister::E);
    decoder[0x9C] = res_n_r(3, WordRegister::H);
    decoder[0x9D] = res_n_r(3, WordRegister::L);
    decoder[0x9E] = res_n_ptr_hl(3);
    decoder[0x9F] = res_n_r(3, WordRegister::A);

    decoder[0xA0] = res_n_r(4, WordRegister::B);
    decoder[0xA1] = res_n_r(4, WordRegister::C);
    decoder[0xA2] = res_n_r(4, WordRegister::D);
    decoder[0xA3] = res_n_r(4, WordRegister::E);
    decoder[0xA4] = res_n_r(4, WordRegister::H);
    decoder[0xA5] = res_n_r(4, WordRegister::L);
    decoder[0xA6] = res_n_ptr_hl(4);
    decoder[0xA7] = res_n_r(4, WordRegister::A);

    decoder[0xA8] = res_n_r(5, WordRegister::B);
    decoder[0xA9] = res_n_r(5, WordRegister::C);
    decoder[0xAA] = res_n_r(5, WordRegister::D);
    decoder[0xAB] = res_n_r(5, WordRegister::E);
    decoder[0xAC] = res_n_r(5, WordRegister::H);
    decoder[0xAD] = res_n_r(5, WordRegister::L);
    decoder[0xAE] = res_n_ptr_hl(5);
    decoder[0xAF] = res_n_r(5, WordRegister::A);

    decoder[0xB0] = res_n_r(6, WordRegister::B);
    decoder[0xB1] = res_n_r(6, WordRegister::C);
    decoder[0xB2] = res_n_r(6, WordRegister::D);
    decoder[0xB3] = res_n_r(6, WordRegister::E);
    decoder[0xB4] = res_n_r(6, WordRegister::H);
    decoder[0xB5] = res_n_r(6, WordRegister::L);
    decoder[0xB6] = res_n_ptr_hl(6);
    decoder[0xB7] = res_n_r(6, WordRegister::A);

    decoder[0xB8] = res_n_r(7, WordRegister::B);
    decoder[0xB9] = res_n_r(7, WordRegister::C);
    decoder[0xBA] = res_n_r(7, WordRegister::D);
    decoder[0xBB] = res_n_r(7, WordRegister::E);
    decoder[0xBC] = res_n_r(7, WordRegister::H);
    decoder[0xBD] = res_n_r(7, WordRegister::L);
    decoder[0xBE] = res_n_ptr_hl(7);
    decoder[0xBF] = res_n_r(7, WordRegister::A);

    decoder[0xC0] = set_n_r(0, WordRegister::B);
    decoder[0xC1] = set_n_r(0, WordRegister::C);
    decoder[0xC2] = set_n_r(0, WordRegister::D);
    decoder[0xC3] = set_n_r(0, WordRegister::E);
    decoder[0xC4] = set_n_r(0, WordRegister::H);
    decoder[0xC5] = set_n_r(0, WordRegister::L);
    decoder[0xC6] = set_n_ptr_hl(0);
    decoder[0xC7] = set_n_r(0, WordRegister::A);

    decoder[0xC8] = set_n_r(1, WordRegister::B);
    decoder[0xC9] = set_n_r(1, WordRegister::C);
    decoder[0xCA] = set_n_r(1, WordRegister::D);
    decoder[0xCB] = set_n_r(1, WordRegister::E);
    decoder[0xCC] = set_n_r(1, WordRegister::H);
    decoder[0xCD] = set_n_r(1, WordRegister::L);
    decoder[0xCE] = set_n_ptr_hl(1);
    decoder[0xCF] = set_n_r(1, WordRegister::A);

    decoder[0xD0] = set_n_r(2, WordRegister::B);
    decoder[0xD1] = set_n_r(2, WordRegister::C);
    decoder[0xD2] = set_n_r(2, WordRegister::D);
    decoder[0xD3] = set_n_r(2, WordRegister::E);
    decoder[0xD4] = set_n_r(2, WordRegister::H);
    decoder[0xD5] = set_n_r(2, WordRegister::L);
    decoder[0xD6] = set_n_ptr_hl(2);
    decoder[0xD7] = set_n_r(2, WordRegister::A);

    decoder[0xD8] = set_n_r(3, WordRegister::B);
    decoder[0xD9] = set_n_r(3, WordRegister::C);
    decoder[0xDA] = set_n_r(3, WordRegister::D);
    decoder[0xDB] = set_n_r(3, WordRegister::E);
    decoder[0xDC] = set_n_r(3, WordRegister::H);
    decoder[0xDD] = set_n_r(3, WordRegister::L);
    decoder[0xDE] = set_n_ptr_hl(3);
    decoder[0xDF] = set_n_r(3, WordRegister::A);

    decoder[0xE0] = set_n_r(4, WordRegister::B);
    decoder[0xE1] = set_n_r(4, WordRegister::C);
    decoder[0xE2] = set_n_r(4, WordRegister::D);
    decoder[0xE3] = set_n_r(4, WordRegister::E);
    decoder[0xE4] = set_n_r(4, WordRegister::H);
    decoder[0xE5] = set_n_r(4, WordRegister::L);
    decoder[0xE6] = set_n_ptr_hl(4);
    decoder[0xE7] = set_n_r(4, WordRegister::A);

    decoder[0xE8] = set_n_r(5, WordRegister::B);
    decoder[0xE9] = set_n_r(5, WordRegister::C);
    decoder[0xEA] = set_n_r(5, WordRegister::D);
    decoder[0xEB] = set_n_r(5, WordRegister::E);
    decoder[0xEC] = set_n_r(5, WordRegister::H);
    decoder[0xED] = set_n_r(5, WordRegister::L);
    decoder[0xEE] = set_n_ptr_hl(5);
    decoder[0xEF] = set_n_r(5, WordRegister::A);

    decoder[0xF0] = set_n_r(6, WordRegister::B);
    decoder[0xF1] = set_n_r(6, WordRegister::C);
    decoder[0xF2] = set_n_r(6, WordRegister::D);
    decoder[0xF3] = set_n_r(6, WordRegister::E);
    decoder[0xF4] = set_n_r(6, WordRegister::H);
    decoder[0xF5] = set_n_r(6, WordRegister::L);
    decoder[0xF6] = set_n_ptr_hl(6);
    decoder[0xF7] = set_n_r(6, WordRegister::A);

    decoder[0xF8] = set_n_r(7, WordRegister::B);
    decoder[0xF9] = set_n_r(7, WordRegister::C);
    decoder[0xFA] = set_n_r(7, WordRegister::D);
    decoder[0xFB] = set_n_r(7, WordRegister::E);
    decoder[0xFC] = set_n_r(7, WordRegister::H);
    decoder[0xFD] = set_n_r(7, WordRegister::L);
    decoder[0xFE] = set_n_ptr_hl(7);
    decoder[0xFF] = set_n_r(7, WordRegister::A);

    decoder
}

impl Opcode for PrefixCb {
    fn exec(&self, cpu: &mut ComputerUnit) {
        // todo this looks like messy. Better do nothing here and execute the extended instruction in the cpu
        cpu.inc_pc(self.size());
        let pc = cpu.get_pc_register();
        let word = cpu.word_at(pc);
        let ref opcode = self.decoder[word];
        opcode.exec(cpu);
        cpu.cycles = cpu.cycles.wrapping_add(opcode.cycles(cpu));
        cpu.inc_pc(opcode.size() - self.size()); // size is added after
    }

    fn size(&self) -> Size {
        1
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        4
    }

    fn to_string(&self, cpu: &ComputerUnit) -> String {
        let pc = cpu.get_pc_register().wrapping_add(1);
        let word = cpu.word_at(pc);
        let ref instruction = self.decoder[word];
        instruction.to_string(cpu)
    }
}

