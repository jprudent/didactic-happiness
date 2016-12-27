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

struct Load<DESTINATION, SOURCE> {
    destination: DESTINATION,
    source: SOURCE,
    size: Double,
    cycles: Cycle
}

enum RegisterOperand {
    A,
    B,
    C,
    D,
    E,
    H,
    L
}

struct ImmediateOperand(Word);

enum MemoryPointerOperand {
    HL
}

impl Opcode for Load<RegisterOperand, ImmediateOperand> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let ImmediateOperand(immediate_value) = self.source;
        match self.destination {
            RegisterOperand::A => panic!("This case doesn't exists"),
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

impl Opcode for Load<RegisterOperand, RegisterOperand> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let word = match self.source {
            RegisterOperand::A => cpu.get_a_register(),
            RegisterOperand::B => cpu.get_b_register(),
            RegisterOperand::C => cpu.get_c_register(),
            RegisterOperand::D => cpu.get_d_register(),
            RegisterOperand::E => cpu.get_e_register(),
            RegisterOperand::H => cpu.get_h_register(),
            RegisterOperand::L => cpu.get_l_register(),
        };
        match self.destination {
            RegisterOperand::A => cpu.set_register_a(word),
            RegisterOperand::B => cpu.set_register_b(word),
            RegisterOperand::C => cpu.set_register_c(word),
            RegisterOperand::D => cpu.set_register_d(word),
            RegisterOperand::E => cpu.set_register_e(word),
            RegisterOperand::H => cpu.set_register_h(word),
            RegisterOperand::L => cpu.set_register_l(word),
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

impl Opcode for Load<MemoryPointerOperand, RegisterOperand> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let word = match self.source {
            RegisterOperand::A => cpu.get_a_register(),
            RegisterOperand::B => cpu.get_b_register(),
            RegisterOperand::C => cpu.get_c_register(),
            RegisterOperand::D => cpu.get_d_register(),
            RegisterOperand::E => cpu.get_e_register(),
            RegisterOperand::H => cpu.get_h_register(),
            RegisterOperand::L => cpu.get_l_register(),
        };

        match self.destination {
            MemoryPointerOperand::HL => {
                let address = cpu.get_hl_register();
                cpu.set_word_at(address, word)
            }
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

impl Opcode for Load<RegisterOperand, MemoryPointerOperand> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let word = match self.source {
            MemoryPointerOperand::HL => cpu.word_at(cpu.get_hl_register())
        };
        match self.destination {
            RegisterOperand::A => cpu.set_register_a(word),
            RegisterOperand::B => cpu.set_register_b(word),
            RegisterOperand::C => cpu.set_register_c(word),
            RegisterOperand::D => cpu.set_register_d(word),
            RegisterOperand::E => cpu.set_register_e(word),
            RegisterOperand::H => cpu.set_register_h(word),
            RegisterOperand::L => cpu.set_register_l(word),
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

trait Opcode {
    fn exec(&self, cpu: &mut ComputerUnit);
}

fn between(word: Word, lower_bound: Word, upper_bound: Word) -> bool {
    word >= lower_bound && word <= upper_bound
}

impl SwitchBasedDecoder {
    fn decode(&self, word: Word, cpu: &ComputerUnit) -> Box<Opcode> {
        if between(word, 0x70, 0x75) {
            let ld_ptr_hl_r = Load {
                destination: MemoryPointerOperand::HL,
                source: RegisterOperand::A,
                size: 1,
                cycles: 8
            };

            Box::new(
                match word {
                    0x70 => Load { source: RegisterOperand::B, ..ld_ptr_hl_r },
                    0x71 => Load { source: RegisterOperand::C, ..ld_ptr_hl_r },
                    0x72 => Load { source: RegisterOperand::D, ..ld_ptr_hl_r },
                    0x73 => Load { source: RegisterOperand::E, ..ld_ptr_hl_r },
                    0x74 => Load { source: RegisterOperand::H, ..ld_ptr_hl_r },
                    0x75 => Load { source: RegisterOperand::L, ..ld_ptr_hl_r },
                    _ => panic!(format!("unhandled opcode : 0x{:02X}", word))
                })
        } else if (word <= 0x2E) && ((word & 0b111) == 0b110) {
            let ld_r_w = Load {
                destination: RegisterOperand::B,
                source: ImmediateOperand(cpu.word_at(cpu.get_pc_register() + 1)),
                size: 2,
                cycles: 8
            };

            Box::new(
                match word {
                    0x06 => Load { destination: RegisterOperand::B, ..ld_r_w },
                    0x0E => Load { destination: RegisterOperand::C, ..ld_r_w },
                    0x16 => Load { destination: RegisterOperand::D, ..ld_r_w },
                    0x1E => Load { destination: RegisterOperand::E, ..ld_r_w },
                    0x26 => Load { destination: RegisterOperand::H, ..ld_r_w },
                    0x2E => Load { destination: RegisterOperand::L, ..ld_r_w },
                    _ => panic!(format!("unhandled opcode : 0x{:02X}", word))
                })
        } else if (word >= 0x46) && (word <= 0x7E) && ((word & 0b111) == 0b110) {
            let ld_r_ptr_hl = Load {
                destination: RegisterOperand::A,
                source: MemoryPointerOperand::HL,
                size: 1,
                cycles: 8
            };
            Box::new(
                match word {
                    0x7E => Load { destination: RegisterOperand::A, ..ld_r_ptr_hl },
                    0x46 => Load { destination: RegisterOperand::B, ..ld_r_ptr_hl },
                    0x4E => Load { destination: RegisterOperand::C, ..ld_r_ptr_hl },
                    0x56 => Load { destination: RegisterOperand::D, ..ld_r_ptr_hl },
                    0x5E => Load { destination: RegisterOperand::E, ..ld_r_ptr_hl },
                    0x66 => Load { destination: RegisterOperand::H, ..ld_r_ptr_hl },
                    0x6E => Load { destination: RegisterOperand::L, ..ld_r_ptr_hl },
                    _ => panic!(format!("unhandled opcode : 0x{:02X}", word))
                })
        } else {
            let ld_r_r = Load {
                destination: RegisterOperand::A,
                source: RegisterOperand::A,
                size: 1,
                cycles: 4
            };
            Box::new(
                match word {
                    0x7F => Load { destination: RegisterOperand::A, source: RegisterOperand::A, ..ld_r_r },
                    0x78 => Load { destination: RegisterOperand::A, source: RegisterOperand::B, ..ld_r_r },
                    0x79 => Load { destination: RegisterOperand::A, source: RegisterOperand::C, ..ld_r_r },
                    0x7A => Load { destination: RegisterOperand::A, source: RegisterOperand::D, ..ld_r_r },
                    0x7B => Load { destination: RegisterOperand::A, source: RegisterOperand::E, ..ld_r_r },
                    0x7C => Load { destination: RegisterOperand::A, source: RegisterOperand::H, ..ld_r_r },
                    0x7D => Load { destination: RegisterOperand::A, source: RegisterOperand::L, ..ld_r_r },

                    0x40 => Load { destination: RegisterOperand::B, source: RegisterOperand::B, ..ld_r_r },
                    0x41 => Load { destination: RegisterOperand::B, source: RegisterOperand::C, ..ld_r_r },
                    0x42 => Load { destination: RegisterOperand::B, source: RegisterOperand::D, ..ld_r_r },
                    0x43 => Load { destination: RegisterOperand::B, source: RegisterOperand::E, ..ld_r_r },
                    0x44 => Load { destination: RegisterOperand::B, source: RegisterOperand::H, ..ld_r_r },
                    0x45 => Load { destination: RegisterOperand::B, source: RegisterOperand::L, ..ld_r_r },

                    0x48 => Load { destination: RegisterOperand::C, source: RegisterOperand::B, ..ld_r_r },
                    0x49 => Load { destination: RegisterOperand::C, source: RegisterOperand::C, ..ld_r_r },
                    0x4A => Load { destination: RegisterOperand::C, source: RegisterOperand::D, ..ld_r_r },
                    0x4B => Load { destination: RegisterOperand::C, source: RegisterOperand::E, ..ld_r_r },
                    0x4C => Load { destination: RegisterOperand::C, source: RegisterOperand::H, ..ld_r_r },
                    0x4D => Load { destination: RegisterOperand::C, source: RegisterOperand::L, ..ld_r_r },

                    0x50 => Load { destination: RegisterOperand::D, source: RegisterOperand::B, ..ld_r_r },
                    0x51 => Load { destination: RegisterOperand::D, source: RegisterOperand::C, ..ld_r_r },
                    0x52 => Load { destination: RegisterOperand::D, source: RegisterOperand::D, ..ld_r_r },
                    0x53 => Load { destination: RegisterOperand::D, source: RegisterOperand::E, ..ld_r_r },
                    0x54 => Load { destination: RegisterOperand::D, source: RegisterOperand::H, ..ld_r_r },
                    0x55 => Load { destination: RegisterOperand::D, source: RegisterOperand::L, ..ld_r_r },

                    0x58 => Load { destination: RegisterOperand::E, source: RegisterOperand::B, ..ld_r_r },
                    0x59 => Load { destination: RegisterOperand::E, source: RegisterOperand::C, ..ld_r_r },
                    0x5A => Load { destination: RegisterOperand::E, source: RegisterOperand::D, ..ld_r_r },
                    0x5B => Load { destination: RegisterOperand::E, source: RegisterOperand::E, ..ld_r_r },
                    0x5C => Load { destination: RegisterOperand::E, source: RegisterOperand::H, ..ld_r_r },
                    0x5D => Load { destination: RegisterOperand::E, source: RegisterOperand::L, ..ld_r_r },

                    0x60 => Load { destination: RegisterOperand::H, source: RegisterOperand::B, ..ld_r_r },
                    0x61 => Load { destination: RegisterOperand::H, source: RegisterOperand::C, ..ld_r_r },
                    0x62 => Load { destination: RegisterOperand::H, source: RegisterOperand::D, ..ld_r_r },
                    0x63 => Load { destination: RegisterOperand::H, source: RegisterOperand::E, ..ld_r_r },
                    0x64 => Load { destination: RegisterOperand::H, source: RegisterOperand::H, ..ld_r_r },
                    0x65 => Load { destination: RegisterOperand::H, source: RegisterOperand::L, ..ld_r_r },

                    0x68 => Load { destination: RegisterOperand::L, source: RegisterOperand::B, ..ld_r_r },
                    0x69 => Load { destination: RegisterOperand::L, source: RegisterOperand::C, ..ld_r_r },
                    0x6A => Load { destination: RegisterOperand::L, source: RegisterOperand::D, ..ld_r_r },
                    0x6B => Load { destination: RegisterOperand::L, source: RegisterOperand::E, ..ld_r_r },
                    0x6C => Load { destination: RegisterOperand::L, source: RegisterOperand::H, ..ld_r_r },
                    0x6D => Load { destination: RegisterOperand::L, source: RegisterOperand::L, ..ld_r_r },

                    _ => panic!(format!("unhandled opcode : 0x{:02X}", word))
                }
            )
        }
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

    fn set_word_at(&mut self, address: Address, word: Word) {
        self.words[address as usize] = word;
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

    fn get_hl_register(&self) -> Double {
        self.registers.hl
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

    fn set_word_at(&mut self, address: Address, word: Word) {
        self.memory.set_word_at(address, word);
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
        }),
    );

    for case in cases {
        let mut cpu = new_cpu();
        cpu.load(&case.program());
        cpu.run_1_instruction();
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

    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.get_b_register(), 0xBB, "bad right register value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 168, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.get_a_register(), 0xBB, "bad left register value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 172, "bad cycles count after {}", msg);
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
    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.get_c_register(), 0xEF, "bad register value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 168, "bad cycles count after {}", msg);
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
    cpu.memory.words[0xABCD] = 0xEF;

    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.word_at(cpu.get_hl_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 168, "bad cycles count after {}", msg);
}