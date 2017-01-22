use super::{Word, Cycle};

#[derive(Eq, PartialEq, Debug)]
enum Period {
    Hz4096 = 1024,
    Hz16384 = 256,
    Hz65536 = 64,
    Hz262144 = 16
}

struct SimpleTimer {
    clock: Cycle,
    counter: Word,
    period: Cycle
}

type Overflow = bool;

impl SimpleTimer {
    pub fn new(increment_each: Period) -> SimpleTimer {
        SimpleTimer {
            clock: 0,
            counter: 0,
            period: increment_each as Cycle
        }
    }

    fn counter(&self) -> Word {
        self.counter
    }

    fn reset(&mut self) {
        self.counter = 0;
    }

    fn set(& mut self, val: Word) {
        self.counter = val;
    }

    fn inc_counter(&mut self, modulo: Word) -> Overflow {
        self.counter = match self.counter.checked_add(1) {
            Some(v) => v,
            None => modulo
        };
        self.counter == modulo
    }

    fn change_period(&mut self, period: Period) {
        self.period = period as Cycle
    }

    fn update(&mut self, elapsed: Cycle) -> Overflow {
        self.update_modulo(elapsed, 0)
    }

    fn update_modulo(&mut self, elapsed: Cycle, modulo: Word) -> Overflow {
        assert!(elapsed > 0);
        let new_clock = (self.clock + elapsed) % self.period;
        let overflow = new_clock <= self.clock && self.inc_counter(modulo);
        self.clock = new_clock;
        overflow
    }
}

pub mod divider {
    use std::cell::RefCell;
    use {Device, Cycle, Address, Word};
    use memory::{MemoryBacked};
    use super::{Period, SimpleTimer};

    pub struct DividerTimer {
        timer: RefCell<SimpleTimer>
    }

    impl DividerTimer {
        pub fn new() -> DividerTimer {
            DividerTimer {
                timer: RefCell::new(SimpleTimer::new(Period::Hz16384))
            }
        }
    }

    impl Device for DividerTimer {
        fn synchronize(&self, cpu_cycles: Cycle) {
            let mut timer = self.timer.borrow_mut();
            (*timer).update(cpu_cycles);
        }
    }

    impl MemoryBacked for DividerTimer {
        fn word_at(&self, _: Address) -> Word {
            self.timer.borrow().counter()
        }

        fn set_word_at(&self, _: Address, _: Word) {
            self.timer.borrow_mut().reset()
        }
    }

    mod test {
        use super::DividerTimer;
        use super::super::super::{Device};
        use memory::MemoryBacked;

        #[test]
        fn should_update_the_timer_when_it_reaches_its_period() {
            let divider_timer = DividerTimer::new();
            divider_timer.synchronize(200);
            assert_eq!(divider_timer.word_at(0xFF04), 0);

            divider_timer.synchronize(200);
            assert_eq!(divider_timer.word_at(0xFF04), 1);
        }

        #[test]
        fn writing_any_value_should_reset_it() {
            let divider_timer = DividerTimer::new();
            divider_timer.synchronize(200);
            divider_timer.synchronize(200);
            assert_eq!(divider_timer.word_at(0xFF04), 1);

            divider_timer.set_word_at(0xFF04, 42);
            assert_eq!(divider_timer.word_at(0xFF04), 0);
        }
    }
}

pub mod timer {
    use super::{Period, SimpleTimer};
    use {Device, Cycle, Word, Address};
    use interrupts::InterruptRequestRegister;
    use memory::{MutableWord, MemoryBacked};
    use std::cell::RefCell;


    pub struct Timer<'a> {
        modulo: MutableWord,
        control: MutableWord,
        interrupt_request_register: &'a InterruptRequestRegister,
        timer: RefCell<SimpleTimer>
    }

    impl<'a> Timer<'a> {
        pub fn new(interrupt_request_register: &'a InterruptRequestRegister) -> Timer<'a> {
            Timer {
                modulo: MutableWord::new(0),
                control: MutableWord::new(0),
                interrupt_request_register: interrupt_request_register,
                timer: RefCell::new(SimpleTimer::new(Period::Hz4096))
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

        fn is_enabled(&self) -> bool {
            self.control.get() & 0b100 == 0b100
        }
    }

    impl<'a> Device for Timer<'a> {
        fn synchronize(&self, cpu_cycles: Cycle) {
            if !self.is_enabled() {
                return;
            }

            let mut timer = self.timer.borrow_mut();

            let period = self.get_period();
            (*timer).change_period(period);

            let overflow = (*timer).update_modulo(cpu_cycles, self.modulo.get());
            if overflow {
                self.interrupt_request_register.request_timer_interrupt();
            }

        }
    }

    impl<'a> MemoryBacked for Timer<'a> {
        fn word_at(&self, address: Address) -> Word {
            match address {
                0xFF05 => (*self.timer.borrow()).counter(),
                0xFF06 => self.modulo.get(),
                0xFF07 => self.control.get(),
                _ => panic!("wrong address mapping in timer at {:04X}", address)
            }
        }

        fn set_word_at(&self, address: Address, word: Word) {
            match address {
                0xFF05 => (*self.timer.borrow_mut()).set(word),
                0xFF06 => self.modulo.set(word),
                0xFF07 => self.control.set(word),
                _ => panic!("wrong address mapping in timer at {:04X}", address)
            };
            println!("addr {:04X}, word: {:02X}", address, word)
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

        assert!(!interrupt_request_register.timer(), "assuming no timer interrupt request");

        //Given the timer is enabled and is incremented every 16 cycles
        timer.set_word_at(0xFF07, 0b101);

        // Given the timer is almost overflowing
        timer.set_word_at(0xFF05, 0xFF);

        // Given the modulo is 42
        timer.set_word_at(0xFF06, 42); // set modulo


        timer.synchronize(16);


        assert_eq!(timer.word_at(0xFF05), 42, "When the TIMA overflows, modulo is loaded.");
        assert!(interrupt_request_register.timer(), "When the TIMA overflows, an interrupt is requested");
    }

    #[test]
    fn when_the_timer_is_stopped_nothing_happens() {
        let interrupt_request_register = InterruptRequestRegister::new();
        let timer = Timer::new(&interrupt_request_register);
        timer.set_word_at(0xFF07, 0b001); // disable + Hz262144
        timer.synchronize(16);
        assert_eq!(timer.word_at(0xFF05), 0, "timer is not incremented because it is disabled");
    }
}
