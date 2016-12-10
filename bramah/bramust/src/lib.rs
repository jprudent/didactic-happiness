pub mod bytes;
pub mod sha256;
pub mod hmac;

use std::ascii::AsciiExt;

fn encode_to_ascii(hash: &Vec<u32>) -> String {
    let range = '~' as u8 - '!' as u8;

    let mut ret = String::new();
    for v in bytes::vec_u32_to_vec_u8(&hash) {
        let c: char = ((v % range) + '!' as u8) as char;
        assert!(c.is_ascii());
        ret.push(c)
    }
    ret
}

#[test]
fn should_encode_to_ascii() {
    assert_eq!(encode_to_ascii(&vec![0x01020304, 0xFCFDFEFF]), "\"#$%cdef".to_string())
}

fn bramah(s: &str, k: &[u8;16]) -> String {
    encode_to_ascii(&hmac::hmac_sha256(s.as_bytes().to_vec(), k.to_vec()))
}

#[test]
fn should_mimic_the_bramah() {
    assert_eq!(bramah("google", &[0_u8;16]), "^E$af2@\'}W:)PuK\\)YErn;,?AiZ<Kl^_".to_string())
}