extern crate piston;
extern crate graphics;
extern crate glutin_window;
extern crate opengl_graphics;

use self::cpu::ComputerUnit;
use self::cpu::Decoder;
use self::interrupts::{InterruptHandler, INTERRUPTS};
use self::debug::{ExecHook, MemoryWriteHook};
use self::program::file_loader;
use self::timer::{DividerTimer};

mod cpu;
mod display;
pub mod debug;
mod lcd;
mod program;
mod interrupts;
mod memory;

//TODO types are duplicated in CPU
pub type Word = u8;
type Double = u16;
pub type Address = Double;
pub type Cycle = u16;

trait Device {
    // todo a device should only depends on cycles and memory
    fn update(&mut self, cpu: &mut ComputerUnit);
}

mod timer {
    use super::{Device, Cycle, Word};
    use super::cpu::ComputerUnit;
    use super::cpu::Opcode;

    enum Period {
        Hz16384 = 256
    }

    pub struct DividerTimer {
        last_cpu_cycles: Cycle,
        counter: Word
    }

    impl DividerTimer {
        pub fn new() -> DividerTimer {
            DividerTimer {
                last_cpu_cycles: 0,
                counter: 0
            }
        }
    }

    impl Device for DividerTimer {
        fn update(&mut self, cpu: &mut ComputerUnit) {
            let cpu_cycles = cpu.cycles() % Period::Hz16384 as Cycle;

            if self.last_cpu_cycles > cpu_cycles {
                self.counter = self.counter.wrapping_add(1);
            }
            self.last_cpu_cycles = cpu_cycles;
        }
    }

    mod test {
        use super::*;
        use super::super::cpu::{Size, ComputerUnit, Opcode};
        use super::super::{Cycle, Device};

        struct FakeInstr(Cycle);

        impl Opcode for FakeInstr {
            fn exec(&self, cpu: &mut ComputerUnit) {}

            fn size(&self) -> Size {
                0
            }

            fn cycles(&self, _: &ComputerUnit) -> Cycle {
                self.0
            }

            fn to_string(&self, cpu: &ComputerUnit) -> String {
                "fake".to_string()
            }
        }

        #[test]
        fn should_update_the_timer_when_it_reaches_its_period() {
            let mut timer = DividerTimer::new();
            let mut cpu = ComputerUnit::new();

            let opcode: Box<Opcode> = Box::new(FakeInstr(200));

            cpu.exec(&opcode);
            timer.update(&mut cpu);
            assert_eq!(timer.counter, 0);

            cpu.exec(&opcode);
            timer.update(&mut cpu);
            assert_eq!(timer.counter, 1)
        }

        #[test]
        fn should_not_update_the_timer_until_it_reaches_its_period() {
            let timer = DividerTimer::new();
            let mut cpu = ComputerUnit::new();

            let opcode: Box<Opcode> = Box::new(FakeInstr(4));

            cpu.exec(&opcode);
            assert_eq!(timer.counter, 0);

            cpu.exec(&opcode);
            assert_eq!(timer.counter, 0)
        }
    }
}

pub fn play(rompath: &str) {
    let pg_loader = file_loader(&rompath.to_string());
    let pg = pg_loader.load();

    let mut cpu = ComputerUnit::new();

    cpu.load(&pg);
    cpu.set_register_pc(0x100);

    let mut gb = GameBoy {
        cpu: cpu,
        interrupt_handler: INTERRUPTS,
        devices: vec!(),
        cpu_hooks: vec!(),
    };

    gb.game_loop();
}

pub fn run_debug<'a>(rompath: &str,
                     cpu_hooks: Vec<&'a mut ExecHook>,
                     memory_hooks: Vec<&'a mut MemoryWriteHook>) {
    let pg_loader = file_loader(&rompath.to_string());
    let pg = pg_loader.load();

    let mut cpu = ComputerUnit::hooked(memory_hooks);

    cpu.load(&pg);
    cpu.set_register_pc(0x100);

    let mut gb = GameBoy {
        cpu: cpu,
        interrupt_handler: INTERRUPTS,
        devices: vec!(Box::new(DividerTimer::new())),
        cpu_hooks: cpu_hooks,
    };

    gb.game_loop();
}

struct GameBoy<'a> {
    cpu: ComputerUnit<'a>,
    cpu_hooks: Vec<&'a mut ExecHook>,
    interrupt_handler: InterruptHandler,
    devices: Vec<Box<Device>>,
}

impl<'a> GameBoy<'a> {
    #[allow(while_true)]
    fn game_loop(&mut self) {
        let decoder = Decoder::new_basic();
        while true {
            self.one_cycle(&decoder);
        }
    }

    fn one_cycle(&mut self, decoder: &Decoder) {
        let word = self.cpu.fetch();

        let opcode = self.cpu.decode(word, decoder);

        for hook in self.cpu_hooks.iter_mut() {
            hook.apply(&self.cpu, opcode);
        }

        self.cpu.exec(opcode);

        for device in self.devices.iter_mut() {
            device.update(&mut self.cpu);
        }

        self.interrupt_handler.interrupt(&mut self.cpu);
    }
}
