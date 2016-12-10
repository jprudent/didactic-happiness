pub fn u64_to_vec_u8(n: u64) -> Vec<u8> {
    (0..8).map(|i| (n >> ((7 - i) * 8) & 0xFF) as u8).collect()
}

#[test]
fn should_convert_a_u64_to_a_vec_of_u8() {
    assert_eq!(u64_to_vec_u8(0x1122334455667788), vec![0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88])
}


pub fn u32_to_vec_u8(n: &u32) -> Vec<u8> {
    (0..4).map(|i| (*n >> ((3 - i) * 8) & 0xFF) as u8).collect()
}

#[test]
fn should_convert_a_u32_to_a_vec_of_u8() {
    assert_eq!(u32_to_vec_u8(&0x11223344), vec![0x11, 0x22, 0x33, 0x44])
}

pub fn vec_u32_to_vec_u8(v: &Vec<u32>) -> Vec<u8> {
    v.iter().flat_map(u32_to_vec_u8).collect()
}

pub fn hexstring_to_vec_u8(s: &str) -> Vec<u8> {
    let as_int:Vec<u8> = s.chars().map(|c| c.to_digit(16).unwrap() as u8).collect();
    let hexa_pairs: Vec<&[u8]> = as_int.chunks(2).collect();
    hexa_pairs.iter().map(|pair| pair[1] + 16 * pair[0]).collect()
}

#[test]
fn should_convert_an_hexstring_to_vec_of_u8() {
    assert_eq!(hexstring_to_vec_u8("01ABFF"), vec![0x01, 0xAB, 0xFF])
}