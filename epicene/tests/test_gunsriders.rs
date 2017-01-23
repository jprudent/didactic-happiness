extern crate epicene;

use epicene::{Address, Word};
use epicene::debug::{MemoryWriteHook};
use epicene::run_debug;

fn test_rom(path: &str) {

    run_debug(
        &path.to_string(),
        vec!(),
        vec!());
}


#[test]
fn should_run_gunsriders() {
    test_rom("roms/gunsriders.gb")
}
