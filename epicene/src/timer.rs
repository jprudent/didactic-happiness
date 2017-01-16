use super::{Device, Cycle, Word, Address, MemoryInterface};
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

impl MemoryInterface for DividerTimer {
    fn word_at(&self, address: Address) -> Word {
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
    use super::super::cpu::{Size, ComputerUnit, Opcode, Registers};
    use super::super::{Cycle, Device};
    use super::super::memory::{Mmu, Ram};

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
        timer.update(0);
        assert_eq!(timer.counter, 0);

        timer.update(200);
        assert_eq!(timer.counter, 0);

        timer.update(255);
        assert_eq!(timer.counter, 0);

        timer.update(256);
        assert_eq! (timer.counter, 1);

        timer.update(400);
        assert_eq! (timer.counter, 1)
    }
}
