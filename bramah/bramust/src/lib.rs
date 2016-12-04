#[cfg(test)]
mod sha256 {
    mod primes {
        fn mantissa(f: f64) -> u32 {
            (f.fract() * 2_f64.powi(32)).trunc() as u32
        }

        #[test]
        fn can_extract_mantissa() {
            assert_eq!(mantissa(2.0), 0);
            assert_eq!(mantissa(2_f64.sqrt()), 0x6a09e667)
        }

        fn is_prime(n: &u32) -> bool {
            let is_not_divisible = |quotient| n % quotient != 0;
            (2..*n).all(is_not_divisible)
        }

        fn primes(how_many: usize) -> Vec<u32> {
            (2..).filter(is_prime).take(how_many).collect()
        }

        #[test]
        fn can_find_primes() {
            assert_eq!(primes(4), vec![2, 3, 5, 7]);
            assert_eq!(primes(64).len(), 64)
        }

        pub fn primes_square_root_mantissa(how_many: usize) -> Vec<u32> {
            primes(how_many)
                .iter()
                .map(|prime| mantissa((*prime as f64).sqrt()))
                .collect()
        }

        pub fn primes_cube_root_mantissa(how_many: usize) -> Vec<u32> {
            primes(how_many)
                .iter()
                .map(|prime| mantissa((*prime as f64).cbrt()))
                .collect()
        }

        #[test]
        fn can_find_primes_square_root_mantissa() {
            assert_eq!(primes_square_root_mantissa(2), vec![0x6a09e667, 0xbb67ae85])
        }

        #[test]
        fn can_find_primes_cube_root_mantissa() {
            assert_eq!(primes_cube_root_mantissa(2), vec![0x428a2f98, 0x71374491])
        }
    }

    mod padding {
        fn u64_to_vec_u8(n: u64) -> Vec<u8> {
            (0..8).map(|i| (n >> ((7 - i) * 8) & 0xFF) as u8).collect()
        }

        #[test]
        fn should_convert_a_u64_to_a_vec_of_u8() {
            assert_eq!(u64_to_vec_u8(0x1122334455667788), vec![0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88])
        }

        use std::iter;

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
    }

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

        for i in 0..64 {
            println!("w {} : {:x}", i, w[i]);
        }
        println!("");
        w
    }

    fn print_h(t: usize, h: &Vec<u32>) {
        print!("t = {} : ", t);
        for i in 0..8 {
            print!("{:x}, ", h[i]);
        }
        println!("")
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
        println!("h: {:?}, chunk: {:?}", h, chunk);
        let w = make_w(chunk);
        let mut h2 = h.clone();
        let k: Vec<u32> = primes::primes_cube_root_mantissa(64);
        for i in 0..64 {
            print_h(i, &h2);
            println!("k = {:x}, w = {:x}", k[i], w[i]);
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
    fn should_hash_a_message() {
        assert_eq!(hash(vec![0x61, 0x62, 0x63]), vec![0xba7816bf, 0x8f01cfea, 0x414140de, 0x5dae2223, 0xb00361a3, 0x96177a9c, 0xb410ff61, 0xf20015ad])
    }
}
