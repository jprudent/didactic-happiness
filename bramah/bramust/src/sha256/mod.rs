mod primes;
mod padding;

#[cfg(test)]
use std::iter;

fn make_w(chunk: &[u8]) -> [u32; 64] {
    assert_eq!(chunk.len(), 64);
    let mut w = [0_u32; 64];
    for word_count in 0..16 {
        for byte_count in 0..4 {
            let chunk_index = word_count * 4 + byte_count;
            w[word_count] = w[word_count] | ((chunk[chunk_index] as u32) << ((3 - byte_count) * 8));
        }
    }

    for word_count in 16..64 {
        let w0 = w[word_count - 15];
        let s0 = w0.rotate_right(7) ^ w0.rotate_right(18) ^ w0.overflowing_shr(3).0;

        let w1 = w[word_count - 2];
        let s1 = w1.rotate_right(17) ^ w1.rotate_right(19) ^ w1.overflowing_shr(10).0;

        w[word_count] = w[word_count - 16]
            .overflowing_add(s0).0
            .overflowing_add(w[word_count - 7]).0
            .overflowing_add(s1).0;
    }

    w
}

fn ch(x: u32, y: u32, z: u32) -> u32 {
    (x & y) ^ (!x & z)
}

fn maj(x: u32, y: u32, z: u32) -> u32 {
    (x & y) ^ (x & z) ^ (y & z)
}

fn s0(x: u32) -> u32 {
    x.rotate_right(2) ^ x.rotate_right(13) ^ x.rotate_right(22)
}

fn s1(x: u32) -> u32 {
    x.rotate_right(6) ^ x.rotate_right(11) ^ x.rotate_right(25)
}

//Will h with content be cloned at each iteration ?
fn hash_chunk(h: Vec<u32>, chunk: &[u8]) -> Vec<u32> {
    assert_eq!(h.len(), 8);
    assert_eq!(chunk.len(), 64);
    let w = make_w(chunk);
    let mut h2 = h.clone();
    let k: Vec<u32> = primes::primes_cube_root_mantissa(64);
    for i in 0..64 {
        // TODO a fold would be nice !
        let temp1 = h2[7]
            .overflowing_add(s1(h2[4])).0
            .overflowing_add(ch(h2[4], h2[5], h2[6])).0
            .overflowing_add(k[i]).0
            .overflowing_add(w[i]).0;

        let temp2 = s0(h2[0])
            .overflowing_add(maj(h2[0], h2[1], h2[2])).0;

        h2[7] = h2[6];
        h2[6] = h2[5];
        h2[5] = h2[4];
        h2[4] = h2[3].overflowing_add(temp1).0;
        h2[3] = h2[2];
        h2[2] = h2[1];
        h2[1] = h2[0];
        h2[0] = temp1.overflowing_add(temp2).0;
    }

    for i in 0..8 {
        h2[i] = h2[i].overflowing_add(h[i]).0;
    }

    assert_eq!(h2.len(), 8);
    h2
}

pub fn hash(original_msg: Vec<u8>) -> Vec<u32> {
    // waste of memory and time copying the original message in another location
    let mut msg = original_msg.to_vec();
    padding::pad(&mut msg);
    msg.chunks(64).fold(primes::primes_square_root_mantissa(8), hash_chunk)
}

#[test]
fn operators_should_work() {
    assert_eq!(!0b11111101_u8, 0b10);
    assert_eq!(255_u8.overflowing_add(2).0, 1)
}

#[test]
fn should_hash_any_message() {
    assert_eq!(hash(vec![0x61, 0x62, 0x63]), vec![0xba7816bf, 0x8f01cfea, 0x414140de, 0x5dae2223, 0xb00361a3, 0x96177a9c, 0xb410ff61, 0xf20015ad]);
    assert_eq!(hash(vec![]), vec![0xe3b0c442, 0x98fc1c14, 0x9afbf4c8, 0x996fb924, 0x27ae41e4, 0x649b934c, 0xa495991b, 0x7852b855]);
    assert_eq!(hash(iter::repeat(0x61).take(1_000_000).collect()), vec![0xcdc76e5c, 0x9914fb92, 0x81a1c7e2, 0x84d73e67, 0xf1809a48, 0xa497200e, 0x046d39cc, 0xc7112cd0])
}