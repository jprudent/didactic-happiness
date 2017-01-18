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
use self::memory::Mmu;

mod cpu;
mod display;
pub mod debug;
mod lcd;
mod program;
mod interrupts;
mod memory;

pub type Word = u8;
type Double = u16;
pub type Address = Double;
pub type Cycle = u16;

trait Device {
    fn synchronize(&self, cpu_cycles: Cycle);
}

mod timer;

pub fn run_debug<'a>(rompath: &str,
                     cpu_hooks: Vec<&'a mut ExecHook>,
                     memory_hooks: Vec<&'a mut MemoryWriteHook>) {
    let pg_loader = file_loader(&rompath.to_string());
    let mut pg = pg_loader.load();
    let mmu = Mmu::new(&mut pg);
    let mut cpu = ComputerUnit::new(memory_hooks, mmu);

    cpu.set_register_pc(0x100);

    let divider_timer = DividerTimer::new();

    let mut gb = GameBoy {
        cpu: cpu,
        interrupt_handler: INTERRUPTS,
        devices: vec!(&divider_timer),
        cpu_hooks: cpu_hooks,
    };

    gb.game_loop();
}

struct GameBoy<'a, 'b, 'device> {
    cpu: ComputerUnit<'a, 'b>,
    cpu_hooks: Vec<&'a mut ExecHook>,
    interrupt_handler: InterruptHandler,
    devices: Vec<&'device Device>,
}

impl<'a, 'b, 'device> GameBoy<'a, 'b, 'device> {
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
            device.synchronize(self.cpu.cycles());
        }

        self.interrupt_handler.interrupt(&mut self.cpu);
    }
}
