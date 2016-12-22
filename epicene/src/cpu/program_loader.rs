use cpu::ProgramLoader;
use cpu::Program;
use cpu::Word;

pub struct MemoryProgramLoader {
    words: Vec<Word>
}

impl ProgramLoader<Vec<Word>> for MemoryProgramLoader {
    fn load(&self, input: Vec<Word>) -> Program {
        Program {
            name: "memory",
            content: input
        }
    }
}