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
