#[derive(Eq, PartialEq, Debug)]
enum Period {
    Hz4096 = 1024,
    Hz16384 = 256,
    Hz65536 = 64,
    Hz262144 = 16
}

pub mod divider {
    use std::cell::RefCell;
    use super::super::{Device, Cycle, Word};
    use super::super::memory::MutableWord;
    use super::Period;

    pub struct DividerTimer {
        last_cpu_cycles: RefCell<Cycle>,
        counter: MutableWord
    }

    impl DividerTimer {
        pub fn new() -> DividerTimer {
            DividerTimer {
                last_cpu_cycles: RefCell::new(0),
                counter: MutableWord::new(0)
            }
        }
    }

    impl Device for DividerTimer {
        fn synchronize(&self, cpu_cycles: Cycle) {
            let cpu_cycles = cpu_cycles % Period::Hz16384 as Cycle;

            let last_cpu_cycles = *self.last_cpu_cycles.borrow();
            if last_cpu_cycles > cpu_cycles {
                let counter = self.counter.get();
                self.counter.set(counter.wrapping_add(1));
            }
            let mut last_cpu_cycles = self.last_cpu_cycles.borrow_mut();
            *last_cpu_cycles = cpu_cycles;
        }
    }

    mod test {
        use super::DividerTimer;
        use super::super::super::{Device};

        #[test]
        fn should_update_the_timer_when_it_reaches_its_period() {
            let divider_timer = DividerTimer::new();
            divider_timer.synchronize(200);
            assert_eq!(*divider_timer.counter.borrow(), 0);

            divider_timer.synchronize(400);
            assert_eq!(*divider_timer.counter.borrow(), 1);

            divider_timer.synchronize(0);
            let v = *divider_timer.counter.borrow();
            assert_eq!(v, 2)
        }
    }
}

pub mod timer {
    use super::Period;
    use super::super::{Device, Cycle, Word, Address};
    use super::super::interrupts::InterruptRequestRegister;
    use super::super::memory::{MutableWord, MemoryBacked};
    use std::cell::RefCell;


    pub struct Timer<'a> {
        last_cpu_cycles: RefCell<Cycle>,
        counter: MutableWord,
        modulo: MutableWord,
        control: MutableWord,
        interrupt_request_register: &'a InterruptRequestRegister
    }

    impl<'a> Timer<'a> {
        pub fn new(interrupt_request_register: &'a InterruptRequestRegister) -> Timer<'a> {
            Timer {
                last_cpu_cycles: RefCell::new(0),
                counter: MutableWord::new(0),
                modulo: MutableWord::new(0),
                control: MutableWord::new(0),
                interrupt_request_register: interrupt_request_register,
            }
        }

        fn set_period(&self, period: Period) {
            let control = self.control.get();
            let new_control = (control & !0b11) | match period {
                Period::Hz4096 => 0b00,
                Period::Hz16384 => 0b11,
                Period::Hz65536 => 010,
                Period::Hz262144 => 0b01,
            };
            self.control.set(new_control);
        }

        fn get_period(&self) -> Period {
            let control = self.control.get();
            let masked = control & 0b11;
            if masked == 0b00 {
                Period::Hz4096
            } else if masked == 0b11 {
                Period::Hz16384
            } else if masked == 0b10 {
                Period::Hz65536
            } else if masked == 0b01 {
                Period::Hz262144
            } else {
                panic!("Can't infer frequency from {:02X}", control)
            }
        }
    }

    impl<'a> Device for Timer<'a> {
        fn synchronize(&self, cpu_cycles: Cycle) {
            let period = self.get_period() as Cycle;
            let mut last = self.last_cpu_cycles.borrow_mut();
            let diff = if cpu_cycles >= *last {
                // no cycle overflow occured
                cpu_cycles - *last
            } else {
                // there was an overflow
                (u16::max_value() - *last) + cpu_cycles
            };
            let counter = self.counter.get();
            let step = (diff / period) as u8;
            println!("diff {}, period {}, step {}, last {}, cpu {}, counter {}", diff, period, step, *last, cpu_cycles, counter);
            if step > 0 {
                match counter.checked_add(step) {
                    Some(v) => self.counter.set(v),
                    None => {
                        self.counter.set(self.modulo.get());
                        self.interrupt_request_register.request_timer_interrupt()
                    }
                };
                *last = cpu_cycles
            }
        }
    }

    impl<'a> MemoryBacked for Timer<'a> {
        fn word_at(&self, address: Address) -> Word {
            match address {
                0xFF05 => &self.counter,
                0xFF06 => &self.modulo,
                0xFF07 => &self.control,
                _ => panic!("wrong address mapping in timer at {:04X}", address)
            }.get()
        }

        fn set_word_at(&self, address: Address, word: Word) {
            match address {
                0xFF05 => &self.counter,
                0xFF06 => &self.modulo,
                0xFF07 => &self.control,
                _ => panic!("wrong address mapping in timer at {:04X}", address)
            }.set(word);
        }
    }

    #[test]
    fn the_frequency_can_be_chosen() {
        let interrupt_request_register = InterruptRequestRegister::new();
        let timer = Timer::new(&interrupt_request_register);

        timer.set_period(Period::Hz4096);
        assert_eq!(timer.get_period(), Period::Hz4096);

        timer.set_period(Period::Hz16384);
        assert_eq!(timer.get_period(), Period::Hz16384);

        timer.set_period(Period::Hz65536);
        assert_eq!(timer.get_period(), Period::Hz65536);

        timer.set_period(Period::Hz262144);
        assert_eq!(timer.get_period(), Period::Hz262144);
    }

    #[test]
    fn when_the_timer_overflows_the_modulo_is_copied_and_an_interrupt_request_is_emited() {
        let interrupt_request_register = InterruptRequestRegister::new();
        let timer = Timer::new(&interrupt_request_register);
        assert!(!interrupt_request_register.timer(), "assuming not timer interrupt request");
        timer.set_period(Period::Hz262144);
        let mut cpu_cycles = 16;
        for _ in 0..0xFF {
            timer.synchronize(cpu_cycles);
            cpu_cycles += 16
        }
        let actual = *timer.counter.borrow();
        assert_eq!(actual, 0xFF);

        timer.modulo.set(42);
        // it will overflow :
        timer.synchronize(16);
        let actual = *timer.counter.borrow();
        assert_eq!(actual, 42, "When the TIMA overflows, modulo is loaded.");
        assert!(interrupt_request_register.timer(), "When the TIMA overflows, an interrupt is requested");
    }

    #[test]
    fn when_the_timer_is_stopped_nothing_happens() {}
}
