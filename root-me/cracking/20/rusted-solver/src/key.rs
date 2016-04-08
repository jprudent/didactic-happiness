const MIN_VAL:i8 = 0;
const MAX_VAL:i8 = 1;

fn inc_vec(a: &mut Vec<i8>, i: usize) -> &mut Vec<i8> {
  if i == a.len() {
    panic!("can't increment");
  }
  if a[i] == MAX_VAL {
    a[i] = MIN_VAL;
    return inc_vec(a, i+1);
  }
  a[i] = a[i] + 1;
  a
}

struct Key {
  a: Vec<i8>
}

impl Key {
  fn inc(&self) -> Key {
    let b: &mut Vec<i8> = &mut self.a;
    let c: Vec<i8> = inc_vec(b,0).clone();
    Key {a: c}
  }
}

#[test]
fn key_should_inc() {
  let key = Key {a: vec![0,0]};
  assert_eq!(key.inc().a.len(),2);
}

#[test]
fn learn() {
  let i : usize = 1;
  i+1;
}