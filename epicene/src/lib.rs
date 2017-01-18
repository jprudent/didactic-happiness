extern crate piston;
extern crate graphics;
extern crate glutin_window;
extern crate opengl_graphics;

use self::cpu::ComputerUnit;
use self::cpu::Decoder;
use self::interrupts::{InterruptHandler, INTERRUPTS};
use self::debug::{ExecHook, MemoryWriteHook};
use self::program::{file_loader, Program};
use self::timer::{DividerTimer, TimerCommand};
use self::bus::{MemoryEnd, make_bus};

mod cpu;
mod display;
pub mod debug;
mod lcd;
mod program;
mod interrupts;
mod memory;
mod timer;
mod bus;

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

fn start<'a, 'b>(program: Program,
                 cpu_hooks: Vec<&'a mut ExecHook>,
                 memory_hooks: Vec<&'a mut MemoryWriteHook>) {
    let (device_end, memory_end) = make_bus();
    let timer = DividerTimer::new(device_end);
    timer.start();
    let mut cpu: ComputerUnit = ComputerUnit::new(memory_hooks, &memory_end);

    cpu.load(&program);
    cpu.set_register_pc(0x100);

    let mut gb = GameBoy {
        cpu: cpu,
        interrupt_handler: INTERRUPTS,
        devices: vec!(&memory_end),
        cpu_hooks: cpu_hooks,
    };

    gb.game_loop();
}

pub fn run_debug<'a>(rompath: &str,
                     cpu_hooks: Vec<&'a mut ExecHook>,
                     memory_hooks: Vec<&'a mut MemoryWriteHook>) {
    let pg_loader = file_loader(&rompath.to_string());
    let pg = pg_loader.load();
    start(pg, cpu_hooks, memory_hooks);
}

struct GameBoy<'a, 'b> {
    cpu: ComputerUnit<'a, 'b>,
    cpu_hooks: Vec<&'a mut ExecHook>,
    interrupt_handler: InterruptHandler,
    devices: Vec<&'b MemoryEnd>,
}

impl<'a, 'b> GameBoy<'a, 'b> {
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

        for device in self.devices.iter() {
            device.send(TimerCommand::Update(self.cpu.cycles()));
        }

        self.interrupt_handler.interrupt(&mut self.cpu);
    }
}
