//#[test]
/*fn should_run_gunsriders() {
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
    let mut cpu = ComputerUnit::new();
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
}
*/