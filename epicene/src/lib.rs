extern crate piston;
extern crate graphics;
extern crate glutin_window;
extern crate opengl_graphics;

use self::cpu::{CpuMode, ComputerUnit};
use self::cpu::Decoder;
use self::interrupts::{InterruptHandler, InterruptRequestRegister, InterruptEnableRegister};
use self::debug::{ExecHook, MemoryWriteHook};
use self::program::file_loader;
use self::timer::divider::{DividerTimer};
use self::timer::timer::{Timer};
use self::memory::Mmu;
use self::sound::Sound;
use self::lcd::{Lcd};
use self::lcd::gpu::{GpuMode};
use self::serial::Serial;
use self::joypad::Joypad;
use self::display::{Screen, Display, LcdState};
use self::video::VideoRam;

use std::sync::mpsc::{Sender, SyncSender, channel, sync_channel};

mod cpu;
mod display;
pub mod debug;
mod lcd;
mod program;
mod interrupts;
mod memory;
mod timer;
mod sound;
mod serial;
mod joypad;
mod video;

pub type Word = u8;
type Double = u16;
pub type Address = Double;
//TODO this should be a type that enforces possible values 0<=cycles<=20, and an u8
pub type Cycle = u16;

trait Device {
    fn synchronize(&self, cpu_cycles: Cycle);
}


pub fn run_debug<'a>(rompath: &str,
                     cpu_hooks: Vec<&'a mut ExecHook>,
                     memory_hooks: Vec<&'a mut MemoryWriteHook>) {
    let pg_loader = file_loader(&rompath.to_string());
    let mut pg = pg_loader.load();

    let interrupt_request_register = InterruptRequestRegister::new();
    let interrupt_enable_register = InterruptEnableRegister::new();
    let timer = Timer::new(&interrupt_request_register);
    let sound = Sound::new();
    let lcd = Lcd::new(&interrupt_request_register);
    let serial = Serial::new();
    let joypad = Joypad::new();
    let video_ram = VideoRam::new();

    let mmu = Mmu::new(&mut pg, &timer, &interrupt_request_register, &interrupt_enable_register, &sound, &lcd, &serial, &joypad, &video_ram);

    let mut cpu = ComputerUnit::new(memory_hooks, mmu);


    cpu.set_register_pc(0x100);

    let divider_timer = DividerTimer::new();

    let interrupt_handler = InterruptHandler::new(&interrupt_enable_register, &interrupt_request_register);

    let (tx, rx) = sync_channel(0);
    let display = Display::new(rx);
    display.start();

    let mut gb = GameBoy {
        cpu: cpu,
        interrupt_handler: &interrupt_handler,
        devices: vec!(&divider_timer, &timer, &sound, &lcd, &serial),
        cpu_hooks: cpu_hooks,
        display_tx: tx,
        video_ram: &video_ram,
        lcd: &lcd
    };

    gb.game_loop();
}

struct GameBoy<'hooks, 'mmu, 'device> {
    cpu: ComputerUnit<'hooks, 'mmu>,
    cpu_hooks: Vec<&'hooks mut ExecHook>,
    interrupt_handler: &'device InterruptHandler<'device>,
    devices: Vec<&'device Device>,
    display_tx: SyncSender<Screen>,
    video_ram: &'device VideoRam,
    lcd: &'device Lcd<'device>,
}

impl<'hooks, 'mmu, 'device> GameBoy<'hooks, 'mmu, 'device> {
    #[allow(while_true)]
    fn game_loop(&mut self) {
        let decoder = Decoder::new_basic();
        while true {
            self.one_cycle(&decoder);
        }
    }

    fn one_cycle(&mut self, decoder: &Decoder) {
        match self.cpu.mode() {
            &CpuMode::Run => self.running_cpu(decoder),
            &CpuMode::HaltContinue => self.halted_cpu(),
            &CpuMode::HaltJumpInterruptVector => self.halted_cpu(),
            &CpuMode::HaltBug => self.halted_buggy(),
            _ => panic!("unhandled case of cpu mode")
        }
        self.update_display()
    }

    fn update_display(&self) {
        if self.cpu.cycles() % 456 == 0 {
            let screen = self.build_screen();
            if let Ok(_) = self.display_tx.try_send(screen) {
                //println!("put");
            } else {
                //println!("skipped");
            }
        }
    }

    fn build_screen(&self) -> Screen {
        Screen {
            lcd_state: LcdState {
                line: self.lcd.current_line(),
                all_tiles: self.video_ram.build_tiles()
            },
        }
    }

    fn halted_buggy(&mut self) {
        // TODO FIXME implement this crap
        // TODO (maybe replacing all code that do a get_register_pc() + 1 by a function that knows in this mode this is pc
        self.cpu.enter(CpuMode::HaltContinue)
    }

    fn halted_cpu(&mut self) {
        self.cpu.add_elapsed_cycles(1);
        self.synchronize_devices();
        self.interrupt_handler.process_requested(&mut self.cpu);
    }

    fn running_cpu(&mut self, decoder: &Decoder) {
        let word = self.cpu.fetch();

        let opcode = self.cpu.decode(word, decoder);

        for hook in self.cpu_hooks.iter_mut() {
            hook.apply(&self.cpu, opcode);
        }

        self.cpu.exec(opcode);

        self.synchronize_devices();

        self.interrupt_handler.process_requested(&mut self.cpu);
    }

    fn synchronize_devices(&mut self) {
        for device in self.devices.iter_mut() {
            device.synchronize(self.cpu.elapsed_cycles());
        }
    }
}
