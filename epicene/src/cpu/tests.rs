//#[cfg(test)]
//use super::super::program::{Program, memory_program_loader};
//use super::super::{Word, Double, Address};
//use super::{Decoder, ComputerUnit, Registers};

/*
#[test]
fn should_implement_every_ld_r_w_instructions() {
    trait UseCaseTrait {
        fn program(&self) -> &Program;
        fn assert(&self, ComputerUnit);
    }

    struct UseCase<ASSERTIONS: Fn(ComputerUnit, String) -> ()> {
        opcode: Box<Opcode>,
        assertions: ASSERTIONS
    }

    impl<ASSERTIONS: Fn(ComputerUnit, String) -> ()> UseCaseTrait for UseCase<ASSERTIONS> {
        fn assert(&self, cpu: ComputerUnit) {
            (self.assertions)(cpu, self.opcode.name.to_string());
        }
        fn program(&self) -> &Program {
            &self.opcode
        }
    }

    use super::opcodes::load::*;

    let cases: Vec<Box<UseCaseTrait>> = vec!(
        Box::new(UseCase {
            opcode: ld_a_w(),
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_a_register(), 0x60, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            opcode: ld_b_w(),
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_b_register(), 0x60, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            opcode: ld_c_w(),
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_c_register(), 0xE0, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            opcode: ld_d_w(),
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_d_register(), 0x61, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            opcode: ld_e_w(),
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_e_register(), 0xE1, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            opcode: ld_h_w(),
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_h_register(), 0x62, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            opcode: ld_l_w(),
            assertions: |cpu, msg| {
                assert_eq!(cpu.get_l_register(), 0xE2, "bad register value after {}", msg);
                assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq!(cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            opcode: Program {
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
            opcode: Program {
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
            opcode: Program {
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
            opcode: Program {
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
        let mut cpu = ComputerUnit::new();
        cpu.load(&case.program());
        cpu.exec(&Decoder::new_basic());
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
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.get_b_register(), 0xBB, "bad right register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.get_a_register(), 0xBB, "bad left register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 172, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_c_ptr_hl_instructions() {
    let pg = Program {
        name: "LD C,(HL)",
        content: vec![0x4E]
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.registers.hl = 0xABCD;
    cpu.memory.words[0xABCD] = 0xEF;
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.get_c_register(), 0xEF, "bad register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_a_ptr_bc_instructions() {
    let pg = Program {
        name: "LD A,(BC)",
        content: vec![0x0A]
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.registers.bc = 0xABCD;
    cpu.memory.words[0xABCD] = 0xEF;
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.get_a_register(), 0xEF, "bad register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}


#[test]
fn should_implement_ld_a_ptr_de_instructions() {
    let pg = Program {
        name: "LD A,(DE)",
        content: vec![0x1A]
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.registers.de = 0xABCD;
    cpu.memory.words[0xABCD] = 0xEF;
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.get_a_register(), 0xEF, "bad register value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_ptr_hl_d_instruction() {
    let pg = Program {
        name: "LD (HL),D",
        content: vec![0x72]
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.registers.hl = 0xABCD;
    cpu.registers.de = 0xEF00;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.word_at(cpu.get_hl_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_ptr_c_a_instruction() {
    let pg = Program {
        name: "LD (C), A",
        content: vec![0xE2]
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.set_register_a(0xEF);
    cpu.set_register_c(0xCD);

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
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
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.set_register_c(0xCD);
    cpu.set_word_at(0xFFCD, 0xEF);

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
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
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.set_register_hl(0xABCD);
    cpu.set_word_at(0xABCD, 0xEF);

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.get_a_register(), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_ptr_nn_a_instruction() {
    let pg = Program {
        name: "LD (0xABCD),A",
        content: vec![0xEA, 0xCD, 0xAB] // little endian
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.registers.af = 0xEF00;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.word_at(0xABCD), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 176, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_ptr_hl_a_instruction() {
    let pg = Program {
        name: "LD (HL),A",
        content: vec![0x77]
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.registers.hl = 0xABCD;
    cpu.registers.af = 0xEF00;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.word_at(cpu.get_hl_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_ptr_bc_a_instruction() {
    let pg = Program {
        name: "LD (BC),A",
        content: vec![0x02]
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.registers.bc = 0xABCD;
    cpu.registers.af = 0xEF00;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.word_at(cpu.get_bc_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_ptr_de_a_instruction() {
    let pg = Program {
        name: "LD (DE),A",
        content: vec![0x12]
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.registers.de = 0xABCD;
    cpu.registers.af = 0xEF00;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.word_at(cpu.get_de_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 168, "bad cycles count after {}", msg);
}

#[test]
fn should_implement_ld_ptr_hl_n_instruction() {
    let pg = Program {
        name: "LD (HL),0x66",
        content: vec![0x36, 0x66]
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.registers.hl = 0xABCD;

    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.word_at(cpu.get_hl_register()), 0x66, "bad memory value after {}", msg);
    assert_eq!(cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
    assert_eq!(cpu.cycles, 172, "bad cycles count after {}", msg);
}


#[test]
fn should_implement_ld_a_ptr_nn_instruction() {
    let pg = Program {
        name: "LD A,(0xABCD)",
        content: vec![0xFA, 0xCD, 0xAB] // little endian
    };

    let msg = pg.name;
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.memory.words[0xABCD] = 0x66;
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
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
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.set_register_hl(0xABCD);
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
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
    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.set_register_sp(0x1234);
    assert_eq!(cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.exec(&Decoder::new_basic());
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

    let mut cpu = ComputerUnit::new();
    cpu.load(&pg);
    cpu.set_register_b(0);
    assert_eq!(cpu.cycles, 160);

    cpu.exec(&Decoder::new_basic());
    assert_eq!(cpu.get_b_register(), 0xFF);
    assert_eq!(cpu.get_pc_register(), 1);
    assert!(!cpu.zero_flag());
    assert!(cpu.add_sub_flag());
    assert_eq!(cpu.cycles, 164);
}

#[test]
fn should_implement_inc() {
    let test_cases = vec!(
        //   sp  flgs      sp
        (0x0000, 0xF0, 0x0001),
        (0x0001, 0xF0, 0x0002),
        (0x000F, 0xF0, 0x0010),
        (0x001F, 0xF0, 0x0020),
        (0x007F, 0xF0, 0x0080),
        (0x0080, 0xF0, 0x0081),
        (0x00FF, 0xF0, 0x0100),
    );

    for case in test_cases {
        let (sp, flags, expected_sp) = case;
        let pg = Program {
            name: "INC SP",
            content: vec![0x33]
        };
        let mut cpu = ComputerUnit::new();
        cpu.load(&pg);
        cpu.set_register_sp(sp);
        cpu.set_register_af(flags as Double);
        cpu.exec(&Decoder::new_basic());
        assert_eq!(cpu.get_sp_register(), expected_sp, "sp={:04X}", sp);
        assert_eq!(cpu.get_af_register(), flags as Double, "sp={:04X}. Flags are left untouched.", sp)
    }
}

#[test]
fn should_implement_adc() {
    let test_cases = vec!(
        // a flgs   b   xpctd
        (0x10F0, 0xFF, 0x1030),
        (0x10F0, 0x10, 0x2100),
    );

    for case in test_cases {
        let (af, b, expected_af) = case;
        let pg = Program {
            name: "ADC B",
            content: vec![0x88]
        };
        let mut cpu = ComputerUnit::new();
        cpu.load(&pg);
        cpu.set_register_b(b);
        cpu.set_register_af(af);
        cpu.exec(&Decoder::new_basic());
        assert_eq!(cpu.get_af_register(), expected_af, "af={:04X}, b={:02X}; expected {:04X} got {:04X}", af, b, expected_af, cpu.get_af_register())
    }
}

#[test]
fn should_implement_sbc() {
    let test_cases = vec!(
        // a flgs   b   xpctd
        (0x0000, 0x00, 0x00C0),
        (0x00F0, 0x00, 0xFF70),
        (0x00F0, 0xFF, 0x00F0),
        (0xFFF0, 0xFF, 0xFF70),
    );

    for case in test_cases {
        let (af, b, expected_af) = case;
        let pg = Program {
            name: "SBC B",
            content: vec![0x98]
        };
        let mut cpu = ComputerUnit::new();
        cpu.load(&pg);
        cpu.set_register_b(b);
        cpu.set_register_af(af);
        cpu.exec(&Decoder::new_basic());
        assert_eq!(cpu.get_af_register(), expected_af, "af={:04X}, b={:02X}; expected {:04X} got {:04X}", af, b, expected_af, cpu.get_af_register())
    }
}

#[test]
fn should_implement_rlca() {
    let test_cases = vec!(
        //  a f   xpctd
        (0x0000, 0x0000),
        (0xFFFF, 0xFF10),
        (0xFF00, 0xFF10),
        (0xEE00, 0xDD10),
        (0x0F00, 0x1E00),
        (0xF000, 0xE110),
        (0x00F0, 0x0000),
        (0x01F0, 0x0200),
        (0x7F00, 0xFE00),
    );

    for case in test_cases {
        let (af, expected_af) = case;
        let pg = Program {
            name: "RLCA",
            content: vec![0x07]
        };
        let mut cpu = ComputerUnit::new();
        cpu.load(&pg);
        cpu.set_register_af(af);
        cpu.exec(&Decoder::new_basic());
        assert_eq!(cpu.get_af_register(), expected_af, "af={:04X}, expected {:04X} got {:04X}", af, expected_af, cpu.get_af_register())
    }
}

#[test]
fn should_implement_ld_hl_sp_plus_n() {
    let test_cases = vec!(
        //  n      sp  flgs      hl  flgs
        (0xFF, 0x0000, 0xFF, 0xFFFF, 0x00),
        (0xFF, 0x0001, 0xFF, 0x0000, 0x30),
        (0xFF, 0x000F, 0xFF, 0x000E, 0x30),
        (0xFF, 0x0010, 0xFF, 0x000F, 0x10),
        (0xFF, 0x001F, 0xFF, 0x001E, 0x30),
        (0xFF, 0x007F, 0xFF, 0x007E, 0x30),
        (0xFF, 0x007F, 0xFF, 0x007E, 0x30),
        (0xFF, 0x0080, 0xFF, 0x007F, 0x10),
        (0xFF, 0x0080, 0xFF, 0x007F, 0x10),
        (0xFF, 0x00FF, 0xFF, 0x00FE, 0x30),
        (0xFF, 0x0100, 0xFF, 0x00FF, 0x00),
        (0xFF, 0x0F00, 0xFF, 0x0EFF, 0x00),
        (0xFF, 0x1F00, 0xFF, 0x1EFF, 0x00),
        (0xFF, 0x1000, 0xFF, 0x0FFF, 0x00),
        (0xFF, 0x7FFF, 0xFF, 0x7FFE, 0x30),
        (0xFF, 0x8000, 0xFF, 0x7FFF, 0x00),
        (0xFF, 0xFFFF, 0xFF, 0xFFFE, 0x30),
        (0x01, 0x0000, 0x00, 0x0001, 0x00),
        (0x01, 0x0001, 0x00, 0x0002, 0x00),
        (0x01, 0x000F, 0x00, 0x0010, 0x20),
        (0x01, 0x0010, 0x00, 0x0011, 0x00),
        (0x01, 0x001F, 0x00, 0x0020, 0x20),
        (0x01, 0x007F, 0x00, 0x0080, 0x20),
        (0x01, 0x0080, 0x00, 0x0081, 0x00),
        (0x01, 0x00FF, 0x00, 0x0100, 0x30),
        (0x01, 0x0100, 0x00, 0x0101, 0x00),
        (0x01, 0x0F00, 0x00, 0x0F01, 0x00),
        (0x01, 0x1F00, 0x00, 0x1F01, 0x00),
        (0x01, 0x1000, 0x00, 0x1001, 0x00),
        (0x01, 0x7FFF, 0x00, 0x8000, 0x30),
        (0x01, 0x8000, 0x00, 0x8001, 0x00),
        (0x01, 0xFFFF, 0x00, 0x0000, 0x30),
    );

    for case in test_cases {
        let (n, sp, flags, expected_hl, expected_flags) = case;
        let pg = Program {
            name: "LD HL,(SP+0xn)",
            content: vec![0xF8, n]
        };
        let mut cpu = ComputerUnit::new();
        cpu.load(&pg);
        cpu.set_register_sp(sp);
        cpu.set_register_af(flags as Double);
        cpu.exec(&Decoder::new_basic());
        assert_eq!(cpu.get_hl_register(), expected_hl, "n={:02X}, sp={:04X}", n, sp);
        assert_eq!(cpu.get_af_register(), expected_flags as Double, "n={:02X}, sp={:04X}", n, sp)
    }
}

#[test]
fn should_write_af() {
    let mut registers = Registers::new();
    registers.set_af(0xFFFF);
    assert_eq!(registers.af, 0xFFF0, "bits 0-3 don't really exist")
}
*/