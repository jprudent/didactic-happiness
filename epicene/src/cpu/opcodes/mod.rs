use super::ComputerUnit;

pub mod disable_interrupts;
pub mod xor;
pub mod jr;
pub mod maths;
pub mod inc_dec_16;
pub mod inc_dec;
pub mod call;
pub mod unconditional_jump;
//todo mutualize withe disable interrupts
pub mod enable_interrupts;
pub mod ret_cond;
pub mod nop;
pub mod ret;
pub mod push;
pub mod pop;
pub mod compare;
pub mod not_implemented;
pub mod load;
pub mod prefix_cb;
pub mod daa;
pub mod ccf;
pub mod scf;
pub mod cpl;


#[derive(Debug)]
enum JmpCondition {
    ALWAYS,
    NONZERO,
    ZERO,
    NOCARRY,
    CARRY
}


impl JmpCondition {
    fn matches(&self, cpu: &ComputerUnit) -> bool {
        match *self {
            JmpCondition::ALWAYS => true,
            JmpCondition::NONZERO => !cpu.zero_flag(),
            JmpCondition::ZERO => cpu.zero_flag(),
            JmpCondition::NOCARRY => !cpu.carry_flag(),
            JmpCondition::CARRY => cpu.carry_flag()
        }
    }

    // TODO could use fmt::Display instead
    fn to_string(&self) -> String {
        match *self {
            JmpCondition::ALWAYS => "".to_string(),
            JmpCondition::NONZERO => "nz".to_string(),
            JmpCondition::ZERO => "z".to_string(),
            JmpCondition::NOCARRY => "nc".to_string(),
            JmpCondition::CARRY => "c".to_string()
        }
    }
}

