use cpu::Word;
use cpu::Double;
use cpu::Opcode;
use cpu::Register;
use cpu::Decoder;
use cpu::Cpu;
use cpu::ReadableMemory;

pub struct SwitchBasedDecoder {

}

struct Load<L,R> {
    left_operand: L,
    right_operand: R,
    size: u8,
    cycles: u8
}

enum RegisterOperand {
    A, B
}

struct ImmediateOperand(Word);

impl<CPU:Cpu> Opcode<CPU> for Load<RegisterOperand, ImmediateOperand>  {
    fn exec(&self, cpu: CPU) -> CPU {
        let ImmediateOperand(immediate_value) = self.right_operand;
        match self.left_operand {
            RegisterOperand::A => cpu.set_register_a(immediate_value),
            RegisterOperand::B => cpu.set_register_b(immediate_value)
        }
    }
}

impl<CPU: Cpu> Decoder<CPU> for SwitchBasedDecoder {
    fn decode(&self, word: Word, cpu : CPU) -> Opcode<CPU>  {
        match word {
            Word(0x06) => Load {
                left_operand: RegisterOperand::B,
                right_operand: ImmediateOperand(cpu.memory_at(cpu.get_pc_register().inc())),
                size: 2,
                cycles: 4
            }
        }
    }
}