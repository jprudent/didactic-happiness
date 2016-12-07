use std::iter;

fn u64_to_vec_u8(n: u64) -> Vec<u8> {
    (0..8).map(|i| (n >> ((7 - i) * 8) & 0xFF) as u8).collect()
}

#[test]
fn should_convert_a_u64_to_a_vec_of_u8() {
    assert_eq!(u64_to_vec_u8(0x1122334455667788), vec![0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88])
}

// here `msg` is mutable because it would be a total waste to copy the original message
// in another memory location
pub fn pad(msg: &mut Vec<u8>) -> () {
    let msg_size: u64 = msg.len() as u64;
    msg.push(0b1000_0000);
    let nb_0_padding = (64 - ((msg.len() + 8) % 64)) % 64;
    let zero_padding: Vec<u8> = iter::repeat(0_u8).take(nb_0_padding).collect();
    msg.extend(zero_padding);
    msg.extend(u64_to_vec_u8(msg_size * 8));
    assert!(msg.len() % (512 / 8) == 0)
}

#[test]
fn should_pad_empty_message() {
    let mut empty = Vec::new();
    pad(&mut empty);
    assert_eq!(empty[63], 0);
    assert_eq!(empty[0], 0x80)
}

#[test]
fn should_pad_message_that_is_more_than_56_bytes() {
    let mut msg = iter::repeat(1_u8).take(57).collect();
    pad(&mut msg);
    assert_eq!(msg[127], 57_u8.overflowing_mul(8).0);
    assert_eq!(msg[57], 0x80)
}

#[test]
fn should_pad_message_that_needs_no_0_padding() {
    let mut msg = iter::repeat(1_u8).take(64 - 8 - 1).collect();
    pad(&mut msg);
    assert_eq!(msg[63], (64 - 8 - 1u8).overflowing_mul(8).0);
    assert_eq!(msg[63 - 8], 0x80)
}

