type Word = u8;
type Double = u16;
type Address = Double;
type Cycle = u8;

////////////////////////////////////////////////////////////////////////////////////////////////////
// Register & co

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
    //sp: Double,
    pc: Double
}

impl Registers {
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
}

pub fn new_registers() -> Registers {
    Registers {
        af: 0x1234,
        bc: 0x1234,
        de: 0x1234,
        hl: 0x1234,
        //sp: 0x1234,
        pc: 0x0000
    }
}

#[test]
fn should_get_value_from_registers() {
    let regs = Registers {
        af: 0xAAFF,
        bc: 0xBBCC,
        de: 0xDDEE,
        hl: 0x4411,
        //sp: 0x5678,
        pc: 0x8765
    };
    assert_eq!(regs.af, 0xAAFF);
    assert_eq!(regs.b(), 0xBB);
    assert_eq!(regs.c(), 0xCC);
}


pub struct SwitchBasedDecoder {}

struct Load<L, R> {
    left_operand: L,
    right_operand: R,
    size: Double,
    cycles: Cycle
}

enum RegisterOperand {
    B,
    C,
    D,
    E,
    H,
    L
}

struct ImmediateOperand(Word);

impl Opcode for Load<RegisterOperand, ImmediateOperand> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let ImmediateOperand(immediate_value) = self.right_operand;
        match self.left_operand {
            RegisterOperand::B => cpu.set_register_b(immediate_value),
            RegisterOperand::C => cpu.set_register_c(immediate_value),
            RegisterOperand::D => cpu.set_register_d(immediate_value),
            RegisterOperand::E => cpu.set_register_e(immediate_value),
            RegisterOperand::H => cpu.set_register_h(immediate_value),
            RegisterOperand::L => cpu.set_register_l(immediate_value),
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

trait Opcode {
    fn exec(&self, cpu: &mut ComputerUnit);
}

impl SwitchBasedDecoder {
    fn decode(&self, word: Word, cpu: &ComputerUnit) -> Box<Opcode> {
        let ld_r_nn = Load {
            left_operand: RegisterOperand::B,
            right_operand: ImmediateOperand(cpu.word_at(cpu.get_pc_register() + 1)),
            size: 2,
            cycles: 8
        };

        Box::new(
            match word {
                0x06 => Load { left_operand: RegisterOperand::B, ..ld_r_nn },
                0x0E => Load { left_operand: RegisterOperand::C, ..ld_r_nn },
                0x16 => Load { left_operand: RegisterOperand::D, ..ld_r_nn },
                0x1E => Load { left_operand: RegisterOperand::E, ..ld_r_nn },
                0x26 => Load { left_operand: RegisterOperand::H, ..ld_r_nn },
                0x2E => Load { left_operand: RegisterOperand::L, ..ld_r_nn },
                _ => panic!(format!("unhandled opcode : 0x{:02X}", word))
            })
    }
}

pub struct ArrayBasedMemory {
    words: [Word; 0xFFFF]
}

impl ArrayBasedMemory {
    fn word_at(&self, address: Address) -> Word {
        self.words[address as usize]
    }

    fn map(&mut self, p: &Program) {
        for i in 0..p.content.len() {
            self.words[i] = p.content[i];
        }
    }
}

struct Program {
    name: &'static str,
    content: Vec<Word>
}

struct ComputerUnit {
    registers: Registers,
    memory: ArrayBasedMemory,
    decoder: SwitchBasedDecoder,
    cycles: Cycle
}

impl ComputerUnit {
    fn inc_pc(&mut self, length: Double) {
        self.registers.pc = self.registers.pc + length;
    }

    fn run_1_instruction(&mut self) {
        let word = self.memory.word_at(self.registers.pc);
        let opcode = self.decoder.decode(word, self);
        opcode.exec(self);
    }

    fn load(&mut self, program: &Program) {
        self.memory.map(&program)
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


    fn set_register_b(&mut self, word: Word) {
        self.registers.bc = set_high_word(self.registers.bc, word)
    }

    fn set_register_c(&mut self, word: Word) {
        self.registers.bc = set_low_word(self.registers.bc, word)
    }

    fn set_register_d(&mut self, word: Word) {
        self.registers.de = set_high_word(self.registers.de, word)
    }

    fn set_register_e(&mut self, word: Word) {
        self.registers.de = set_low_word(self.registers.de, word)
    }

    fn set_register_h(&mut self, word: Word) {
        self.registers.hl = set_high_word(self.registers.hl, word)
    }

    fn set_register_l(&mut self, word: Word) {
        self.registers.hl = set_low_word(self.registers.hl, word)
    }

    fn word_at(&self, address: Address) -> Word {
        self.memory.word_at(address)
    }
}

fn new_cpu() -> ComputerUnit {
    ComputerUnit {
        registers: new_registers(),
        memory: ArrayBasedMemory {
            words: [0xAA; 0xFFFF]
        },
        decoder: SwitchBasedDecoder {},
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
    cpu.run_1_instruction();

    assert_eq!(cpu.get_b_register(), 0xBA);
    assert_eq!(cpu.get_pc_register(), 0x02);
}

#[test]
fn should_implement_every_load_instructions() {
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
                name: "LD B, 0x60",
                content: vec![0x06, 0x60]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_b_register(), 0x60, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD C, 0xE0",
                content: vec![0x0E, 0xE0]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_c_register(), 0xE0, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD D, 0x61",
                content: vec![0x16, 0x61]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_d_register(), 0x61, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD E, 0xE1",
                content: vec![0x1E, 0xE1]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_e_register(), 0xE1, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD H, 0x62",
                content: vec![0x26, 0x62]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_h_register(), 0x62, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD L, 0xE2",
                content: vec![0x2E, 0xE2]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_l_register(), 0xE2, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        })
    );

    for case in cases {
        let mut cpu = new_cpu();
        cpu.load(&case.program());
        cpu.run_1_instruction();
        case.assert(cpu);
        // (case.assertions)(cpu, case.program.name.to_string());
    }
}
