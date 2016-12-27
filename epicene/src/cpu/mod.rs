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
    sp: Double,
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
        sp: 0x1234,
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
        sp: 0x5678,
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

enum WordRegister {
    A,
    B,
    C,
    D,
    E,
    H,
    L,
}

enum DoubleRegister {
    BC,
    DE,
    HL,
    SP
}

// TODO generic ? can be deleted altogether
struct ImmediateWord(Word);

struct ImmediateDouble(Double);

struct ImmediateMemoryPointer(Address);

enum MemoryPointerOperand {
    HL,
    BC,
    DE,
    C
}

impl Opcode for Load<WordRegister, ImmediateWord> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let ImmediateWord(immediate_value) = self.source;
        match self.destination {
            WordRegister::A => cpu.set_register_a(immediate_value),
            WordRegister::B => cpu.set_register_b(immediate_value),
            WordRegister::C => cpu.set_register_c(immediate_value),
            WordRegister::D => cpu.set_register_d(immediate_value),
            WordRegister::E => cpu.set_register_e(immediate_value),
            WordRegister::H => cpu.set_register_h(immediate_value),
            WordRegister::L => cpu.set_register_l(immediate_value),
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

impl Opcode for Load<DoubleRegister, ImmediateDouble> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let ImmediateDouble(immediate_value) = self.source;
        match self.destination {
            DoubleRegister::BC => cpu.set_register_bc(immediate_value),
            DoubleRegister::DE => cpu.set_register_de(immediate_value),
            DoubleRegister::HL => cpu.set_register_hl(immediate_value),
            DoubleRegister::SP => cpu.set_register_sp(immediate_value),
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

impl Opcode for Load<WordRegister, WordRegister> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let word = match self.source {
            WordRegister::A => cpu.get_a_register(),
            WordRegister::B => cpu.get_b_register(),
            WordRegister::C => cpu.get_c_register(),
            WordRegister::D => cpu.get_d_register(),
            WordRegister::E => cpu.get_e_register(),
            WordRegister::H => cpu.get_h_register(),
            WordRegister::L => cpu.get_l_register(),
        };
        match self.destination {
            WordRegister::A => cpu.set_register_a(word),
            WordRegister::B => cpu.set_register_b(word),
            WordRegister::C => cpu.set_register_c(word),
            WordRegister::D => cpu.set_register_d(word),
            WordRegister::E => cpu.set_register_e(word),
            WordRegister::H => cpu.set_register_h(word),
            WordRegister::L => cpu.set_register_l(word),
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

impl Opcode for Load<MemoryPointerOperand, WordRegister> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let word = match self.source {
            WordRegister::A => cpu.get_a_register(),
            WordRegister::B => cpu.get_b_register(),
            WordRegister::C => cpu.get_c_register(),
            WordRegister::D => cpu.get_d_register(),
            WordRegister::E => cpu.get_e_register(),
            WordRegister::H => cpu.get_h_register(),
            WordRegister::L => cpu.get_l_register(),
        };

        match self.destination {
            MemoryPointerOperand::HL => {
                let address = cpu.get_hl_register();
                cpu.set_word_at(address, word)
            }
            MemoryPointerOperand::BC => {
                let address = cpu.get_bc_register();
                cpu.set_word_at(address, word)
            }
            MemoryPointerOperand::DE => {
                let address = cpu.get_de_register();
                cpu.set_word_at(address, word)
            }
            MemoryPointerOperand::C => {
                let address = set_low_word(0xFF00, cpu.get_c_register());
                cpu.set_word_at(address, word)
            }
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

impl Opcode for Load<WordRegister, MemoryPointerOperand> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let word = match self.source {
            MemoryPointerOperand::HL => cpu.word_at(cpu.get_hl_register()),
            MemoryPointerOperand::BC => cpu.word_at(cpu.get_bc_register()),
            MemoryPointerOperand::DE => cpu.word_at(cpu.get_de_register()),
            MemoryPointerOperand::C => cpu.word_at(set_low_word(0xFF00, cpu.get_c_register())),
        };
        match self.destination {
            WordRegister::A => cpu.set_register_a(word),
            WordRegister::B => cpu.set_register_b(word),
            WordRegister::C => cpu.set_register_c(word),
            WordRegister::D => cpu.set_register_d(word),
            WordRegister::E => cpu.set_register_e(word),
            WordRegister::H => cpu.set_register_h(word),
            WordRegister::L => cpu.set_register_l(word),
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

impl Opcode for Load<MemoryPointerOperand, ImmediateWord> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let ImmediateWord(word) = self.source;
        match self.destination {
            MemoryPointerOperand::HL => {
                let hl = cpu.get_hl_register();
                cpu.set_word_at(hl, word)
            },
            _ => panic!("should not happen")
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

impl Opcode for Load<WordRegister, ImmediateMemoryPointer> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let ImmediateMemoryPointer(address) = self.source;
        let word = cpu.word_at(address);
        match self.destination {
            WordRegister::A => cpu.set_register_a(word),
            _ => panic!("should not happen")
        }
        cpu.inc_pc(self.size);
        cpu.cycles = cpu.cycles + self.cycles;
    }
}

impl Opcode for Load<ImmediateMemoryPointer, WordRegister> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let word = match self.source {
            WordRegister::A => cpu.get_a_register(),
            _ => panic!("should not happen")
        };
        let ImmediateMemoryPointer(address) = self.destination;
        cpu.set_word_at(address, word);
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
        if word == 0xEA {
            Box::new(
                Load {
                    destination: ImmediateMemoryPointer(cpu.double_at(cpu.get_pc_register() + 1)),
                    source: WordRegister::A,
                    size: 3,
                    cycles: 16
                }
            )
        } else if word == 0xFA {
            Box::new(
                Load {
                    destination: WordRegister::A,
                    source: ImmediateMemoryPointer(cpu.double_at(cpu.get_pc_register() + 1)),
                    size: 3,
                    cycles: 16
                }
            )
        } else if word == 0x36 {
            Box::new(
                Load {
                    destination: MemoryPointerOperand::HL,
                    source: ImmediateWord(cpu.word_at(cpu.get_pc_register() + 1)),
                    size: 1,
                    cycles: 12
                }
            )
        } else if word == 0x02 || word == 0x012 || word == 0xE2 || between(word, 0x70, 0x77) {
            let ld_ptr_hl_r = Load {
                destination: MemoryPointerOperand::HL,
                source: WordRegister::A,
                size: 1,
                cycles: 8
            };

            Box::new(
                match word {
                    0x77 => Load { source: WordRegister::A, ..ld_ptr_hl_r },
                    0x02 => Load { destination: MemoryPointerOperand::BC, source: WordRegister::A, ..ld_ptr_hl_r },
                    0x12 => Load { destination: MemoryPointerOperand::DE, source: WordRegister::A, ..ld_ptr_hl_r },
                    0xE2 => Load { destination: MemoryPointerOperand::C, source: WordRegister::A, ..ld_ptr_hl_r },
                    0x70 => Load { source: WordRegister::B, ..ld_ptr_hl_r },
                    0x71 => Load { source: WordRegister::C, ..ld_ptr_hl_r },
                    0x72 => Load { source: WordRegister::D, ..ld_ptr_hl_r },
                    0x73 => Load { source: WordRegister::E, ..ld_ptr_hl_r },
                    0x74 => Load { source: WordRegister::H, ..ld_ptr_hl_r },
                    0x75 => Load { source: WordRegister::L, ..ld_ptr_hl_r },
                    _ => panic!(format!("unhandled opcode : 0x{:02X}", word))
                })
        } else if ((word <= 0x2E) && ((word & 0b111) == 0b110)) || word == 0x3E {
            let ld_r_w = Load {
                destination: WordRegister::B,
                source: ImmediateWord(cpu.word_at(cpu.get_pc_register() + 1)),
                size: 2,
                cycles: 8
            };

            Box::new(
                match word {
                    0x3E => Load { destination: WordRegister::A, ..ld_r_w },
                    0x06 => Load { destination: WordRegister::B, ..ld_r_w },
                    0x0E => Load { destination: WordRegister::C, ..ld_r_w },
                    0x16 => Load { destination: WordRegister::D, ..ld_r_w },
                    0x1E => Load { destination: WordRegister::E, ..ld_r_w },
                    0x26 => Load { destination: WordRegister::H, ..ld_r_w },
                    0x2E => Load { destination: WordRegister::L, ..ld_r_w },
                    _ => panic!(format!("unhandled opcode : 0x{:02X}", word))
                })
        } else if word == 0x01 || word == 0x11 || word == 0x21 || word == 0x31 {
            let ld_r_w = Load {
                destination: DoubleRegister::BC,
                source: ImmediateDouble(cpu.double_at(cpu.get_pc_register() + 1)),
                size: 3,
                cycles: 12
            };

            Box::new(
                match word {
                    0x01 => Load { destination: DoubleRegister::BC, ..ld_r_w },
                    0x11 => Load { destination: DoubleRegister::DE, ..ld_r_w },
                    0x21 => Load { destination: DoubleRegister::HL, ..ld_r_w },
                    0x31 => Load { destination: DoubleRegister::SP, ..ld_r_w },
                    _ => panic!(format!("unhandled opcode : 0x{:02X}", word))
                })
        } else if ((word >= 0x46) && (word <= 0x7E) && ((word & 0b111) == 0b110)) ||
            word == 0x0A || word == 0x1A || word == 0xF2 {
            let ld_r_ptr_hl = Load {
                destination: WordRegister::A,
                source: MemoryPointerOperand::HL,
                size: 1,
                cycles: 8
            };
            Box::new(
                match word {
                    0x7E => Load { destination: WordRegister::A, ..ld_r_ptr_hl },
                    0x0A => Load { destination: WordRegister::A, source: MemoryPointerOperand::BC, ..ld_r_ptr_hl },
                    0x1A => Load { destination: WordRegister::A, source: MemoryPointerOperand::DE, ..ld_r_ptr_hl },
                    0xF2 => Load { destination: WordRegister::A, source: MemoryPointerOperand::C, ..ld_r_ptr_hl },
                    0x46 => Load { destination: WordRegister::B, ..ld_r_ptr_hl },
                    0x4E => Load { destination: WordRegister::C, ..ld_r_ptr_hl },
                    0x56 => Load { destination: WordRegister::D, ..ld_r_ptr_hl },
                    0x5E => Load { destination: WordRegister::E, ..ld_r_ptr_hl },
                    0x66 => Load { destination: WordRegister::H, ..ld_r_ptr_hl },
                    0x6E => Load { destination: WordRegister::L, ..ld_r_ptr_hl },
                    _ => panic!(format!("unhandled opcode : 0x{:02X}", word))
                })
        } else {
            let ld_r_r = Load {
                destination: WordRegister::A,
                source: WordRegister::A,
                size: 1,
                cycles: 4
            };
            Box::new(
                match word {
                    0x7F => Load { destination: WordRegister::A, source: WordRegister::A, ..ld_r_r },
                    0x78 => Load { destination: WordRegister::A, source: WordRegister::B, ..ld_r_r },
                    0x79 => Load { destination: WordRegister::A, source: WordRegister::C, ..ld_r_r },
                    0x7A => Load { destination: WordRegister::A, source: WordRegister::D, ..ld_r_r },
                    0x7B => Load { destination: WordRegister::A, source: WordRegister::E, ..ld_r_r },
                    0x7C => Load { destination: WordRegister::A, source: WordRegister::H, ..ld_r_r },
                    0x7D => Load { destination: WordRegister::A, source: WordRegister::L, ..ld_r_r },

                    0x40 => Load { destination: WordRegister::B, source: WordRegister::B, ..ld_r_r },
                    0x41 => Load { destination: WordRegister::B, source: WordRegister::C, ..ld_r_r },
                    0x42 => Load { destination: WordRegister::B, source: WordRegister::D, ..ld_r_r },
                    0x43 => Load { destination: WordRegister::B, source: WordRegister::E, ..ld_r_r },
                    0x44 => Load { destination: WordRegister::B, source: WordRegister::H, ..ld_r_r },
                    0x45 => Load { destination: WordRegister::B, source: WordRegister::L, ..ld_r_r },

                    0x48 => Load { destination: WordRegister::C, source: WordRegister::B, ..ld_r_r },
                    0x49 => Load { destination: WordRegister::C, source: WordRegister::C, ..ld_r_r },
                    0x4A => Load { destination: WordRegister::C, source: WordRegister::D, ..ld_r_r },
                    0x4B => Load { destination: WordRegister::C, source: WordRegister::E, ..ld_r_r },
                    0x4C => Load { destination: WordRegister::C, source: WordRegister::H, ..ld_r_r },
                    0x4D => Load { destination: WordRegister::C, source: WordRegister::L, ..ld_r_r },

                    0x50 => Load { destination: WordRegister::D, source: WordRegister::B, ..ld_r_r },
                    0x51 => Load { destination: WordRegister::D, source: WordRegister::C, ..ld_r_r },
                    0x52 => Load { destination: WordRegister::D, source: WordRegister::D, ..ld_r_r },
                    0x53 => Load { destination: WordRegister::D, source: WordRegister::E, ..ld_r_r },
                    0x54 => Load { destination: WordRegister::D, source: WordRegister::H, ..ld_r_r },
                    0x55 => Load { destination: WordRegister::D, source: WordRegister::L, ..ld_r_r },

                    0x58 => Load { destination: WordRegister::E, source: WordRegister::B, ..ld_r_r },
                    0x59 => Load { destination: WordRegister::E, source: WordRegister::C, ..ld_r_r },
                    0x5A => Load { destination: WordRegister::E, source: WordRegister::D, ..ld_r_r },
                    0x5B => Load { destination: WordRegister::E, source: WordRegister::E, ..ld_r_r },
                    0x5C => Load { destination: WordRegister::E, source: WordRegister::H, ..ld_r_r },
                    0x5D => Load { destination: WordRegister::E, source: WordRegister::L, ..ld_r_r },

                    0x60 => Load { destination: WordRegister::H, source: WordRegister::B, ..ld_r_r },
                    0x61 => Load { destination: WordRegister::H, source: WordRegister::C, ..ld_r_r },
                    0x62 => Load { destination: WordRegister::H, source: WordRegister::D, ..ld_r_r },
                    0x63 => Load { destination: WordRegister::H, source: WordRegister::E, ..ld_r_r },
                    0x64 => Load { destination: WordRegister::H, source: WordRegister::H, ..ld_r_r },
                    0x65 => Load { destination: WordRegister::H, source: WordRegister::L, ..ld_r_r },

                    0x68 => Load { destination: WordRegister::L, source: WordRegister::B, ..ld_r_r },
                    0x69 => Load { destination: WordRegister::L, source: WordRegister::C, ..ld_r_r },
                    0x6A => Load { destination: WordRegister::L, source: WordRegister::D, ..ld_r_r },
                    0x6B => Load { destination: WordRegister::L, source: WordRegister::E, ..ld_r_r },
                    0x6C => Load { destination: WordRegister::L, source: WordRegister::H, ..ld_r_r },
                    0x6D => Load { destination: WordRegister::L, source: WordRegister::L, ..ld_r_r },

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

    fn double_at(&self, address: Address) -> Double {
        let i = address as usize;
        set_high_word(set_low_word(0, self.words[i]), self.words[i + 1])
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

    fn word_at(&self, address: Address) -> Word {
        self.memory.word_at(address)
    }

    fn double_at(&self, address: Address) -> Double {
        self.memory.double_at(address)
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
                name: "LD A, 0x60",
                content: vec![0x3E, 0x60]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_a_register(), 0x60, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x02, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 0xA8, "bad cycles count after {}", msg);
            }
        }),
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
        Box::new(UseCase {
            program: Program {
                name: "LD BC, 0xABCD",
                content: vec![0x01, 0xCD, 0xAB]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_bc_register(), 0xABCD, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 172, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD DE, 0xABCD",
                content: vec![0x11, 0xCD, 0xAB]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_de_register(), 0xABCD, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 172, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD HL, 0xABCD",
                content: vec![0x21, 0xCD, 0xAB]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_hl_register(), 0xABCD, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 172, "bad cycles count after {}", msg);
            }
        }),
        Box::new(UseCase {
            program: Program {
                name: "LD SP, 0xABCD",
                content: vec![0x31, 0xCD, 0xAB]
            },
            assertions: |cpu, msg| {
                assert_eq! (cpu.get_sp_register(), 0xABCD, "bad register value after {}", msg);
                assert_eq! (cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
                assert_eq! (cpu.cycles, 172, "bad cycles count after {}", msg);
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
    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.get_a_register(), 0xEF, "bad register value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 168, "bad cycles count after {}", msg);
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
    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.get_a_register(), 0xEF, "bad register value after {}", msg);
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

    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.word_at(cpu.get_hl_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 168, "bad cycles count after {}", msg);
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

    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.word_at(0xFFCD), 0xEF, "bad memory value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 168, "bad cycles count after {}", msg);
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

    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.get_a_register(), 0xEF, "bad memory value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 168, "bad cycles count after {}", msg);
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

    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.word_at(0xABCD), 0xEF, "bad memory value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 176, "bad cycles count after {}", msg);
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

    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.word_at(cpu.get_hl_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 168, "bad cycles count after {}", msg);
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

    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.word_at(cpu.get_bc_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 168, "bad cycles count after {}", msg);
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

    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.word_at(cpu.get_de_register()), 0xEF, "bad memory value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 168, "bad cycles count after {}", msg);
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

    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.word_at(cpu.get_hl_register()), 0x66, "bad memory value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x01, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 172, "bad cycles count after {}", msg);
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
    assert_eq! (cpu.cycles, 160, "bad cycles count after {}", msg);

    cpu.run_1_instruction();
    assert_eq! (cpu.get_a_register(), 0x66, "bad register value after {}", msg);
    assert_eq! (cpu.get_pc_register(), 0x03, "bad pc after {}", msg);
    assert_eq! (cpu.cycles, 176, "bad cycles count after {}", msg);
}