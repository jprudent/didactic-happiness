use super::{Word, Address};
use super::cpu::{Opcode, ComputerUnit};

pub trait ExecHook {
    fn apply(&mut self, &ComputerUnit, &Box<Opcode>);
}

pub trait MemoryWriteHook {
    fn apply(&mut self, Address, Word);
}

pub struct CpuLogger;


impl CpuLogger {
    fn instruction_bytes(cpu: &ComputerUnit, opcode: &Box<Opcode>) -> String {
        let mut s = "".to_string();
        for i in 0..opcode.size() {
            let w = cpu.word_at(cpu.get_pc_register().wrapping_add(i));
            s.push_str(&format!("{:02X}", w))
        }
        s
    }
}

use std::fs::File;

pub struct CpuFileLogger {
    file: File
}

#[allow(dead_code)]
pub fn cpu_file_logger(filename: &str) -> CpuFileLogger {
    CpuFileLogger {
        file: File::create(filename).unwrap()
    }
}

impl ExecHook for CpuFileLogger {
    fn apply(&mut self, cpu: &ComputerUnit, opcode: &Box<Opcode>) {
        use std::io::{Write};
        self.file.write(format_cpu(cpu, opcode).as_bytes()).unwrap();
        self.file.write("\n".as_bytes()).unwrap();
        self.file.flush().unwrap();
    }
}

fn format_cpu(cpu: &ComputerUnit, opcode: &Box<Opcode>) -> String {
    format!("@{:04X} {:<6}|{:<10}|af={:04X}|bc={:04X}|de={:04X}|hl={:04X}|sp={:04X}|{}{}{}{}|{:02X}|{:02X}",
            cpu.get_pc_register(),
            CpuLogger::instruction_bytes(cpu, opcode),
            opcode.to_string(cpu),
            cpu.get_af_register(),
            cpu.get_bc_register(),
            cpu.get_de_register(),
            cpu.get_hl_register(),
            cpu.get_sp_register(),
            if cpu.zero_flag() { "Z" } else { "z" },
            if cpu.add_sub_flag() { "N" } else { "n" },
            if cpu.half_carry_flag() { "H" } else { "h" },
            if cpu.carry_flag() { "C" } else { "c" },
            cpu.word_at(0xFF0F),
            cpu.word_at(0xFFFF),
    )
}

impl ExecHook for CpuLogger {
    fn apply(&mut self, cpu: &ComputerUnit, opcode: &Box<Opcode>) {
        println!("{}", format_cpu(cpu, opcode))
    }
}

#[allow(dead_code)]
pub fn on_write(address: Address, hook: Box<MemoryWriteHook>) -> Box<MemoryWriteHook> {
    Box::new(OnWrite(address, hook))
}

struct OnWrite(Address, Box<MemoryWriteHook>);

impl MemoryWriteHook for OnWrite {
    fn apply(&mut self, address: Address, word: Word) {
        if address == self.0 {
            self.1.apply(address, word)
        }
    }
}

#[allow(dead_code)]
pub fn print_char() -> Box<MemoryWriteHook> {
    Box::new(PrintChar)
}

struct PrintChar;

impl MemoryWriteHook for PrintChar {
    fn apply(&mut self, _: Address, word: Word) {
        print!("{}", word as char);
    }
}


#[allow(dead_code)]
pub fn on_exec(instruction: Word, hook: Box<ExecHook>) -> Box<ExecHook> {
    Box::new(OnExec(instruction, hook))
}

struct OnExec(Word, Box<ExecHook>);

impl ExecHook for OnExec {
    fn apply(&mut self, cpu: &ComputerUnit, opcode: &Box<Opcode>) {
        if cpu.word_at(cpu.get_pc_register()) == self.0 {
            self.1.apply(cpu, opcode)
        }
    }
}

#[allow(dead_code)]
pub fn when_at(address: Address, hook: Box<ExecHook>) -> Box<ExecHook> {
    Box::new(WhenAt(address, hook))
}

struct WhenAt(Address, Box<ExecHook>);

impl ExecHook for WhenAt {
    fn apply(&mut self, cpu: &ComputerUnit, opcode: &Box<Opcode>) {
        if cpu.get_pc_register() == self.0 {
            self.1.apply(cpu, opcode)
        }
    }
}

#[allow(dead_code)]
pub fn serial_monitor() -> Box<MemoryWriteHook> {
    let sb = 0xFF01;
    on_write(sb, print_char())
}

