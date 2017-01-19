



enum Period {
    Hz16384 = 256
}

pub mod divider {
    use std::cell::RefCell;
    use super::super::{Device, Cycle, Word};
    use super::Period;

    pub struct DividerTimer {
        last_cpu_cycles: RefCell<Cycle>,
        counter: RefCell<Word>
    }

    impl DividerTimer {
        pub fn new() -> DividerTimer {
            DividerTimer {
                last_cpu_cycles: RefCell::new(0),
                counter: RefCell::new(0)
            }
        }
    }

    impl Device for DividerTimer {
        fn synchronize(&self, cpu_cycles: Cycle) {
            let cpu_cycles = cpu_cycles % Period::Hz16384 as Cycle;

            let last_cpu_cycles = *self.last_cpu_cycles.borrow();
            if last_cpu_cycles > cpu_cycles {
                let mut counter = self.counter.borrow_mut();
                *counter = counter.wrapping_add(1);
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

mod timer {

}
