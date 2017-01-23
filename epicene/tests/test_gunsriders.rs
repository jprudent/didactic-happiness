extern crate epicene;

use epicene::run_debug;

fn test_rom(path: &str) {

    run_debug(
        &path.to_string(),
        vec!(),
        vec!());
}


#[test]
fn should_run_gunsriders() {
    test_rom("/home/stup3fait/games/gb/TETRIS.GB")
}
