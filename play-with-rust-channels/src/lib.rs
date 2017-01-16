#[cfg(test)]
mod tests {
    use std::sync::mpsc::{sync_channel, SyncSender, Receiver};
    use std::thread;

    enum TimerCommand {
        Read,
        Write(u8),
        Update(u8)
    }

    struct Timer {
        counter: u8,
        command_bus: Receiver<TimerCommand>,
        response_bus: SyncSender<u8>
    }

    impl Timer {
        fn start(mut self) {
            thread::Builder::new().name("Timer".to_string()).spawn(move || {
                while true {
                    let command: TimerCommand = self.command_bus.recv().unwrap();
                    match command {
                        TimerCommand::Read => self.response_bus.send(self.counter).unwrap(),
                        TimerCommand::Write(word) => self.counter = 0,
                        TimerCommand::Update(word) => self.counter += word,
                    }
                }
            });
        }
    }

    struct Memory<'a> {
        timer_command_bus: &'a SyncSender<TimerCommand>,
        timer_response_bus: &'a Receiver<u8>
    }

    impl<'a>  Memory<'a>  {
        fn write_word(&self, word: u8) {
            self.timer_command_bus.send(TimerCommand::Write(word)).unwrap();
        }

        fn read_word(&self) -> u8 {
            self.timer_command_bus.send(TimerCommand::Read).unwrap();
            self.timer_response_bus.recv().unwrap()
        }
    }

    struct Computer<'a> {
        timer_command_bus: &'a SyncSender<TimerCommand>,
        cycles: u8
    }

    impl<'a> Computer<'a> {
        fn inc_cycles(&mut self) {
            self.cycles += 1;
        }

        fn update_devices(&self) {
            self.timer_command_bus.send(TimerCommand::Update(self.cycles)).unwrap();
        }
    }

    #[test]
    fn it_works() {
        let (timer_command_bus_sender, timer_command_bus_receiver) = sync_channel(0);
        let (timer_response_bus_sender, timer_response_bus_receiver) = sync_channel(0);

        {
            let memory = Memory {
                timer_command_bus: &timer_command_bus_sender,
                timer_response_bus: &timer_response_bus_receiver
            };

            let mut timer = Timer {
                counter: 0,
                command_bus: timer_command_bus_receiver,
                response_bus: timer_response_bus_sender,
            };

            let mut computer = Computer {
                cycles: 0,
                timer_command_bus: &timer_command_bus_sender
            };

            timer.start();

            memory.write_word(100);
            assert_eq!(memory.read_word(), 0);

            computer.inc_cycles();
            computer.update_devices();
            assert_eq!(memory.read_word(), 1);

            memory.write_word(2);
            assert_eq!(memory.read_word(), 0);

            computer.inc_cycles();
            computer.update_devices();
            assert_eq!(memory.read_word(), 2);

            computer.inc_cycles();
            computer.update_devices();
            assert_eq!(memory.read_word(), 5);
        }
    }
}
