mod register;
mod memory;
mod decoder;
mod program_loader;

#[cfg(test)]
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
struct Register<V: RegisterValue>(V);

impl<V: RegisterValue + Copy> Register<V> {
    fn value(&self) -> V {
        self.0
    }

    fn inc(&self, v: V) -> Register<V> {
        self.value().inc()
    }
}

trait RegisterValue {
    fn inc(&self) -> RegisterValue;
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
struct Word(u8);

impl RegisterValue for Word {
    fn inc(&self) -> RegisterValue {
        unimplemented!()
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
struct Double(u16);

impl Double {
    fn high_word(&self) -> Word {
        Word(self.0.wrapping_shr(8) as u8)
    }

    fn low_word(&self) -> Word {
        Word((self.0 & 0xFF) as u8)
    }
}

impl RegisterValue for Double {
    fn inc(&self) -> RegisterValue {
        unimplemented!()
    }
}

pub trait ReadableRegisters {
    fn af(&self) -> Register<Double>;
    fn bc(&self) -> Register<Double>;
    fn de(&self) -> Register<Double>;
    fn hl(&self) -> Register<Double>;
    fn sp(&self) -> Register<Double>;
    fn pc(&self) -> Register<Double>;
    fn a(&self) -> Register<Word>;
    fn b(&self) -> Register<Word>;
    fn c(&self) -> Register<Word>;
    fn d(&self) -> Register<Word>;
    fn e(&self) -> Register<Word>;
    fn h(&self) -> Register<Word>;
    fn l(&self) -> Register<Word>;
}

trait Opcode<CPU: Cpu> {
    fn exec(&self, CPU) -> CPU;
}

pub trait Decoder<CPU: Cpu> {
    fn decode(&self, Word, CPU) -> Opcode<CPU>;
}

struct Program {
    name: &'static str,
    content: Vec<Word>
}

trait Cpu {
    fn load(&self, Program) -> Cpu;
    fn run_1_instruction(&self) -> Cpu;
    fn set_register_a(&self, Word) -> Cpu;
    fn set_register_b(&self, Word) -> Cpu;
    fn get_b_register(&self) -> Register<Word>;
    fn get_pc_register(&self) -> Register<Double>;
}

trait ProgramLoader<T> {
    fn load(&self, T) -> Program;
}

type Address = Double;

pub trait ReadableMemory {
    fn word_at(&self, Address) -> Word;
}

trait WritableMemory {
    fn map(&self, p: Program);
}

// implementation of a CPU

struct ComputerUnit {
    registers: register::Registers,
    memory: memory::ArrayBasedMemory,
    decoder: decoder::SwitchBasedDecoder,
}

impl ComputerUnit {
    fn inc_pc(&self, length: Double) {
        self.registers.pc().add(length);
    }
}

impl Cpu for ComputerUnit {
    fn run_1_instruction(&self) -> Cpu {
        let word = self.memory.word_at(self.registers.pc().value());
        let opcode = self.decoder.decode(word, self.memory, self.get_pc_register());
        opcode.exec(self)
    }

    fn load(&self, program: Program) -> Cpu {
        self.memory.map(program)
    }

    fn get_b_register(&self) -> Register<Word> {
        self.registers.b()
    }

    fn get_pc_register(&self) -> Register<Double> {
        self.registers.pc()
    }
    fn set_register_a(&self, _: Word) -> Cpu {
        unimplemented!()
    }

    fn set_register_b(&self, _: Word) -> Cpu {
        unimplemented!()
    }
}

#[test]
fn should_load_program() {
    let cpu = ComputerUnit {
        registers: register::new_register(),
        memory: memory::new_memory(),
        decoder: decoder::SwitchBasedDecoder {}
    };

    let program = program_loader::MemoryProgramLoader {
        words: vec![Word(0x06), Word(0xBA)] // LD B, 0xBA
    };

    let actual_cpu = cpu.load(program).run_1_instruction();

    assert_eq!(actual_cpu.get_b_register(), Register(Word(0xBA)));
    assert_eq!(actual_cpu.get_pc_register(), Register(Double(0x02)));
}
