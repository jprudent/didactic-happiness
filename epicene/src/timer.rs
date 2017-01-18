use super::{Device, Cycle, Word, Address, MemoryInterface};
use super::bus::DeviceEnd;
use std::thread;

enum Period {
    Hz16384 = 256
}

pub enum TimerCommand {
    Read(Address),
    Write(Address, Word),
    Update(Cycle)
}

pub struct DividerTimer {
    last_cpu_cycles: Cycle,
    counter: Word,
    bus: DeviceEnd,
}


impl DividerTimer {
    pub fn new(bus: DeviceEnd) -> DividerTimer {
        DividerTimer {
            last_cpu_cycles: 0,
            counter: 0,
            bus: bus
        }
    }

    pub fn start(mut self) {
        thread::Builder::new().name("Timer".to_string()).spawn(move || {
            while let Some(command) = self.bus.receive() {
                match command {
                    TimerCommand::Read(address) =>
                        self.bus.send(self.word_at(address)),
                    TimerCommand::Write(address, word) =>
                    //Pandocs: Writing any value to this register resets it to 00h
                        self.set_word_at(address, word),
                    TimerCommand::Update(cpu_cycles) =>
                        self.update(cpu_cycles),
                }
            }
        }).unwrap();
    }
}

impl MemoryInterface for DividerTimer {
    fn word_at(&self, _: Address) -> Word {
        self.counter
    }

    fn set_word_at(&mut self, _: Address, _: Word) {
        //Pandocs: Writing any value to this register resets it to 00h
        self.counter = 0
    }
}

impl Device for DividerTimer {
    fn update(&mut self, cycles: Cycle) {
        let cpu_cycles = cycles % Period::Hz16384 as Cycle;
        if self.last_cpu_cycles > cpu_cycles {
            self.counter = self.counter.wrapping_add(1);
        }
        self.last_cpu_cycles = cpu_cycles;
    }
}

mod test {
    use super::*;
    use super::super::bus::make_bus;

    #[test]
    fn should_update_the_timer_when_it_reaches_its_period() {
        let (timer_end, memory_end) = make_bus();

        let timer = DividerTimer::new(timer_end);
        timer.start();

        memory_end.send(TimerCommand::Update(0));
        assert_eq!(memory_end.ask(TimerCommand::Read(1)), 0);

        memory_end.send(TimerCommand::Update(200));
        assert_eq!(memory_end.ask(TimerCommand::Read(1)), 0);

        memory_end.send(TimerCommand::Update(255));
        assert_eq!(memory_end.ask(TimerCommand::Read(1)), 0);

        memory_end.send(TimerCommand::Update(256));
        assert_eq!(memory_end.ask(TimerCommand::Read(1)), 1);

        memory_end.send(TimerCommand::Update(400));
        assert_eq!(memory_end.ask(TimerCommand::Read(1)), 1);
    }

    #[test]
    fn should_reset_the_timer_when_writing_in() {
        let (timer_end, memory_end) = make_bus();

        let timer = DividerTimer::new(timer_end);
        timer.start();

        memory_end.send(TimerCommand::Update(200));
        assert_eq!(memory_end.ask(TimerCommand::Read(1)), 0);

        memory_end.send(TimerCommand::Update(256));
        assert_eq!(memory_end.ask(TimerCommand::Read(1)), 1);

        memory_end.send(TimerCommand::Write(0xF00, 42));
        assert_eq!(memory_end.ask(TimerCommand::Read(1)), 0);
    }
}
