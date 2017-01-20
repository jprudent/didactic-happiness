extern crate epicene;

use epicene::{Address, Word};
use epicene::debug::{MemoryWriteHook, ExecHook, cpu_file_logger};
use epicene::run_debug;


struct WatchTestStatus {
    record: String
}

impl MemoryWriteHook for WatchTestStatus {
    fn apply(&mut self, address: Address, c: Word) {
        if address == 0xFF01 {
            self.record.push(c as char);
            if self.record.contains("Passed") {
                println!("{}", self.record);
                panic!("Test pass");
            } else if self.record.contains("Failed") {
                println!("{}", self.record);
                panic!("Test failed");
            }
        }
    }
}

fn test_rom(path: &str) {
    let mut watch_test_statuc = WatchTestStatus {
        record: "".to_string()
    };

    run_debug(
        &path.to_string(),
        vec!(& mut cpu_file_logger("/tmp/epicene.debug")),
        vec!(&mut watch_test_statuc));
}

#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_01() {
    test_rom("roms/cpu_instrs/individual/01-special.gb");
}


#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_02() {
    test_rom("roms/cpu_instrs/individual/02-interrupts.gb");
}

#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_03() {
    test_rom("roms/cpu_instrs/individual/03-op sp,hl.gb");
}

#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_04() {
    test_rom("roms/cpu_instrs/individual/04-op r,imm.gb");
}

#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_05() {
    test_rom("roms/cpu_instrs/individual/05-op rp.gb");
}

#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_06() {
    test_rom("roms/cpu_instrs/individual/06-ld r,r.gb");
}

#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_07() {
    test_rom("roms/cpu_instrs/individual/07-jr,jp,call,ret,rst.gb");
}

#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_08() {
    test_rom("roms/cpu_instrs/individual/08-misc instrs.gb");
}

#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_09() {
    test_rom("roms/cpu_instrs/individual/09-op r,r.gb");
}

#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_10() {
    test_rom("roms/cpu_instrs/individual/10-bit ops.gb");
}

#[test]
#[should_panic(expected = "Test pass")]
fn should_pass_testrom_11() {
    test_rom("roms/cpu_instrs/individual/11-op a,(hl).gb");
}