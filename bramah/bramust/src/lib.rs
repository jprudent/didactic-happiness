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

        fn primes_mantissa(how_many: usize) -> Vec<u32> {
            primes(how_many)
                .iter()
                .map(|prime| mantissa((*prime as f64).sqrt()))
                .collect()
        }

        #[test]
        fn can_find_primes_mantissa() {
            assert_eq!(primes_mantissa(2), vec![0x6a09e667, 0xbb67ae85])
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

        fn pad(msg: &mut Vec<u8>) -> () {
            let msg_size: u64 = msg.len() as u64;
            println!("original size {}", msg.len());
            msg.push(0b1000_0000);
            println!("after 1 {}", msg.len());
            let nb_0_padding = (64 - ((msg.len() + 8) % 64)) % 64;
            let zero_padding: Vec<u8> = iter::repeat(0_u8).take(nb_0_padding).collect();
            msg.extend(zero_padding);

            msg.extend(u64_to_vec_u8(msg_size));

            println!("padding {}, size {}", nb_0_padding, msg.len());

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
            assert_eq!(msg[127], 57);
            assert_eq!(msg[57], 0x80)
        }

        #[test]
        fn should_pad_message_that_needs_no_0_padding() {
            let mut msg = iter::repeat(1_u8).take(64 - 8 - 1).collect();
            pad(&mut msg);
            assert_eq!(msg[63], 64 - 8 -1);
            assert_eq!(msg[63 - 8], 0x80)
        }
    }

    
}
