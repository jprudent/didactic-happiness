use cpu::Word;
use cpu::Address;
use cpu::Double;
use cpu::ReadableMemory;
use cpu::WritableMemory;
use cpu::Program;

pub struct ArrayBasedMemory {
    words: [Word; 0xFFFF]
}

impl ReadableMemory for ArrayBasedMemory {
    fn word_at(&self, address: Address) -> Word {
        let Double(v) = address;
        self.words[v as usize]
    }
}

impl WritableMemory for ArrayBasedMemory {
    fn map(&self, p: Program) {
        unimplemented!()
    }
}

pub fn new_memory() -> ArrayBasedMemory {
    ArrayBasedMemory {
      words: [Word(0xA); 0xFFFF]
    }
}