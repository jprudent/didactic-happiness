extern crate piston;
extern crate graphics;
extern crate glutin_window;
extern crate opengl_graphics;

use self::cpu::ComputerUnit;
use self::cpu::Decoder;

mod cpu;
mod display;

mod program;

trait Device {
    fn update(&self, cpu: &mut ComputerUnit);
}

struct GameBoy {
    cpu: ComputerUnit,
    interrupt_handler: InterruptHandler,
    devices: Vec<Box<Device>>
}


mod lcd {
    use super::cpu::ComputerUnit;

    struct Lcd {}

    impl Lcd {
        fn update(&self, _: &mut ComputerUnit) {
            //let cycles = cpu.clock();
        }
    }
}

mod interrupts {
    use super::cpu::ComputerUnit;

    pub struct InterruptHandler;

    impl InterruptHandler {
        pub fn interrupt(&self, cpu: &mut ComputerUnit) {
            if cpu.interrupt_master() {
                if cpu.vblank_interrupt_enabled() && cpu.vblank_interrupt_requested() {
                    cpu.disable_interrupt_master();
                    cpu.set_register_pc(0x40)
                }
            }
        }
    }
}

impl GameBoy {
    fn game_loop(&mut self) {
        let decoder = Decoder::new_basic();
        while true {
            self.cpu.run_1_instruction(&decoder);
            for device in &self.devices {
                device.update(&mut self.cpu);
            }
            self.interrupt_handler.interrupt(&mut self.cpu);
        }
    }
}

use self::interrupts::InterruptHandler;

#[test]
fn test_da_gameboy() {
    use self::program::file_loader;
    let pg_loader = file_loader(&"roms/cpu_instrs/individual/01-special.gb".to_string());
    let pg = pg_loader.load();

    let mut exec_hooks: Vec<(Box<ExecHook>)> = vec!();
    //exec_hooks.push(BreakpointFactory::on_exec_addr(0xC302, log_cpu_state));
    use self::cpu::debug::{cpu_logger};
    use self::cpu::{ExecHook, Hooks};
    exec_hooks.push(cpu_logger());
    let mut write_hooks = vec!();
    //write_hooks.push(BreakpointFactory::on_write_addr(0xC302, print_memory_write));

    let mut cpu = ComputerUnit::new_hooked(Hooks {
        before_exec: exec_hooks,
        before_write: write_hooks
    });

    cpu.load(&pg);

    let mut gb = GameBoy {
        cpu: cpu,
        interrupt_handler: InterruptHandler {},
        devices: vec!()
    };

    gb.game_loop();
}