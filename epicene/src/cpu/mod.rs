use std::mem;

type Word = u8;
type Double = u16;
type Address = Double;

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

pub struct Registers {
    af: Double,
    bc: Double,
    de: Double,
    hl: Double,
    sp: Double,
    pc: Double
}

impl Registers {
    fn b(&self) -> Word {
        high_word(self.bc)
    }

    fn c(&self) -> Word {
        low_word(self.bc)
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

struct Load<L, R> {
    left_operand: L,
    right_operand: R,
    size: Double,
    cycles: u8
}

enum RegisterOperand {
    A,
    B
}

struct ImmediateOperand(Word);

impl Opcode for Load<RegisterOperand, ImmediateOperand> {
    fn exec(&self, cpu: &mut ComputerUnit) {
        let ImmediateOperand(immediate_value) = self.right_operand;
        match self.left_operand {
            RegisterOperand::A => cpu.set_register_a(immediate_value),
            RegisterOperand::B => cpu.set_register_b(immediate_value)
        }
        cpu.inc_pc(self.size)
    }
}

trait Opcode {
    fn exec(&self, cpu: &mut ComputerUnit);
}

impl SwitchBasedDecoder {
    fn decode(&self, word: Word, cpu: &ComputerUnit) -> Box<Opcode> {
        Box::new(
            match word {
                0x06 => Load {
                    left_operand: RegisterOperand::B,
                    right_operand: ImmediateOperand(cpu.word_at(cpu.get_pc_register() + 1)),
                    size: 2,
                    cycles: 4
                },
                _ => panic!(format!("unhandled opcode : {:2X}", word))
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

    fn map(&mut self, p: Program) {
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
}

impl ComputerUnit {
    fn inc_pc(&mut self, length: Double) {
        self.registers.pc = self.registers.pc + length;
    }

    fn run_1_instruction(&mut self) {
        let word = self.memory.word_at(self.registers.pc);
        let opcode = self.decoder.decode(word, self);
        opcode.exec(self)
    }

    fn load(&mut self, program: Program) {
        self.memory.map(program)
    }

    fn get_b_register(&self) -> Word {
        self.registers.b()
    }

    fn get_pc_register(&self) -> Double {
        self.registers.pc
    }
    fn set_register_a(&mut self, _: Word) {
        unimplemented!()
    }

    fn set_register_b(&mut self, word: Word) {
        self.registers.bc = set_high_word(self.registers.bc, word)
    }

    fn word_at(&self, address: Address) -> Word {
        self.memory.word_at(address)
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
    let mut cpu = ComputerUnit {
        registers: new_registers(),
        memory: ArrayBasedMemory {
            words: [0xAA; 0xFFFF]
        },
        decoder: SwitchBasedDecoder {}
    };

    let program_loader = MemoryProgramLoader {};
    let program = program_loader.load(vec![0x06, 0xBA]); // LD B, 0xBA

    cpu.load(program);
    cpu.run_1_instruction();

    assert_eq!(cpu.get_b_register(), 0xBA);
    assert_eq!(cpu.get_pc_register(), 0x02);
}