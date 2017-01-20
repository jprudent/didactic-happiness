use super::{Address, Word};
use super::memory::MemoryBacked;
use std::cell::RefCell;

pub struct Program {
    pub name: &'static str,
    pub content: RefCell<Vec<Word>>
}

pub trait ProgramLoader {
    fn load(&self) -> Program;
}

struct MemoryProgramLoader {
    data: Vec<Word>
}

#[allow(dead_code)]
pub fn memory_program_loader(data: &Vec<Word>) -> Box<ProgramLoader> {
    // TODO Clone is really not the thing to do but I can't make reference working (fighting the borrow checker)
    Box::new(MemoryProgramLoader { data: data.clone() })
}


impl ProgramLoader for MemoryProgramLoader {
    fn load(&self) -> Program {
        Program {
            name: "memory",
            // TODO Clone is really not the thing to do but I can't make reference working (fighting the borrow checker)
            content: RefCell::new(self.data.clone())
        }
    }
}


pub fn file_loader(path: &String) -> Box<ProgramLoader> {
    // TODO Clone is really not the thing to do but I can't make reference working (fighting the borrow checker)
    Box::new(FileProgramLoader { path: path.clone() })
}

struct FileProgramLoader {
    path: String
}

impl ProgramLoader for FileProgramLoader {
    fn load(&self) -> Program {
        use std::io::prelude::*;
        use std::fs::File;
        let mut f = File::open(&self.path).unwrap();
        let mut s = vec!();
        f.read_to_end(&mut s).unwrap();
        Program {
            name: "cpu_instrs.gb",
            content: RefCell::new(s)
        }
    }
}

impl MemoryBacked for Program {
    fn word_at(&self, address: Address) -> Word {
        let content = self.content.borrow();
        (*content)[address as usize]
    }

    fn set_word_at(&self, address: Address, word: Word) {
        let mut content = self.content.borrow_mut();
        (*content)[address as usize] = word
    }
}