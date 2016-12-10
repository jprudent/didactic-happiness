use sha256;
use std::iter::repeat;
use bytes;

fn concat(v1: &Vec<u8>, v2: &Vec<u8>) -> Vec<u8> {
    let mut ret = v1.clone();
    ret.extend(v2);
    ret
}

fn xor(v1: &Vec<u8>, v2: &Vec<u8>) -> Vec<u8> {
    v1.iter().zip(v2.iter())
        .map(|t| t.0 ^ t.1)
        .collect()
}

pub fn hmac_sha256(msg: Vec<u8>, key: Vec<u8>) -> Vec<u32> {
    let k:Vec<u8> = if key.len() > 8 {
        sha256::hash(key).iter().flat_map(bytes::u32_to_vec_u8).collect()
    } else {
        concat(&key, &repeat(0_u8).take(64 - key.len()).collect())
    };

    let opad = xor(&repeat(0x5c).take(64).collect(), &k);
    let ipad = xor(&repeat(0x36).take(64).collect(), &k);
    let hash_ipad = sha256::hash(concat(&ipad, &msg)).iter().flat_map(bytes::u32_to_vec_u8).collect();
    sha256::hash(concat(&opad, &hash_ipad))
}

#[test]
fn should_hmac_any_message() {
    assert_eq!(hmac_sha256(vec![], vec![]), vec![0xb613679a, 0x0814d9ec, 0x772f95d7, 0x78c35fc5, 0xff1697c4, 0x93715653, 0xc6c71214, 0x4292c5ad]);
}