use super::super::{Word, Cycle, Size, Opcode, ComputerUnit, Decoder};

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

    decoder
}

impl Opcode for PrefixCb {
    fn exec(&self, cpu: &mut ComputerUnit) {
        cpu.inc_pc(1);
        let pc = cpu.get_pc_register();
        let word = cpu.word_at(pc);
        let ref instruction = self.decoder[word];
        instruction.exec(cpu);
    }

    fn size(&self) -> Size {
        1
    }

    fn cycles(&self, _: &ComputerUnit) -> Cycle {
        4
    }
}

