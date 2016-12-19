pub mod bytes;
pub mod sha256;
pub mod hmac;

use std::env;

fn encode_byte_to_ascii(byte: u8) -> char {
    println!("Encoding {:?}", byte);
    let range = '~' as u8 - '!' as u8;
    ((byte % range) + '!' as u8) as char
}

#[test]
fn should_encode_byte_any_byte_to_ascii() {
    assert_eq!(encode_byte_to_ascii(0), '!');
    assert_eq!(encode_byte_to_ascii(1), '"')
}

fn encode_to_ascii(hash: &Vec<u32>) -> String {
    let mut ret = String::new();
    for v in bytes::vec_u32_to_vec_u8(&hash) {
        let c = encode_byte_to_ascii(v);
        ret.push(c);
    }
    ret
}

#[test]
fn should_encode_to_ascii() {
    assert_eq!(encode_to_ascii(&vec![0x01020304, 0xFCFDFEFF]), "\"#$%cdef".to_string());
    let mut v = vec![0_u32;64];
    for i in 0..64 {
        let mut a_u32 = 0_u32;
        for j in 0..4 {
            let n : u32 = i as u32 * 4 + j;
            a_u32 = a_u32 | n.wrapping_shl(8 * (3 - j));
        }
        v[i] = a_u32;
    }
    assert_eq!(encode_to_ascii(&v), "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdef")

}

fn bramah(s: &str, k: &Vec<u8>) -> String {
    assert_eq!(k.len(), 16);
    encode_to_ascii(&hmac::hmac_sha256(s.as_bytes().to_vec(), k.to_vec()))
}

#[test]
fn should_mimic_the_bramah() {
    assert_eq!(bramah("google", &[0_u8; 16]), "^E$af2@\'}W:)PuK\\)YErn;,?AiZ<Kl^_".to_string())
}

fn main() {
    let args: Vec<_> = env::args().collect();
    assert_eq!(args.len(), 3);
    assert_eq!(args[2].len(), 32);
    let password = bramah(&args[1], &bytes::hexstring_to_vec_u8(&args[2]));
    println!("The password is \u{001B}[0;32;42m{}", password);

}