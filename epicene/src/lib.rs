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
    fn update(&mut self, cycles: Cycle);
}


pub trait MemoryInterface {
    fn word_at(&self, Address) -> Word;
    fn set_word_at(&mut self, Address, Word);
}


mod timer;

pub fn play(rompath: &str) {
    let pg_loader = file_loader(&rompath.to_string());
    let pg = pg_loader.load();
    let mut timer = DividerTimer::new();

    let mut gb = GameBoy {
        interrupt_handler: INTERRUPTS,
        devices: vec!(Box::new(timer)),
        cpu_hooks: vec!(),
        cpu: ComputerUnit::new(&mut timer),
    };

    gb.

    gb.cpu.load(&pg);
    gb.cpu.set_register_pc(0x100);

    gb.game_loop();
}

pub fn run_debug<'a>(rompath: &str,
                     cpu_hooks: Vec<&'a mut ExecHook>,
                     memory_hooks: Vec<&'a mut MemoryWriteHook>) {
    let pg_loader = file_loader(&rompath.to_string());
    let pg = pg_loader.load();
    let mut timer = DividerTimer::new();
    let mut cpu = ComputerUnit::hooked(memory_hooks, &mut timer);

    cpu.load(&pg);
    cpu.set_register_pc(0x100);

    let mut gb = GameBoy {
        cpu: cpu,
        interrupt_handler: INTERRUPTS,
        devices: vec!(Box::new(timer)),
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
            device.update(self.cpu.cycles());
        }

        self.interrupt_handler.interrupt(&mut self.cpu);
    }
}
