
#include "DichotomicKeyboard.h"
#include "unity.h"

#ifdef UNIT_TEST

void test_1_symbol_dichotomic_keyboard_should_always_give_same_letter(void)
{
  Keys * alpha = new Keys("0", 1);
  Keys ** keys = new Keys*[1] {alpha};


  /* All of these should pass */
  DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.left();
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());

  // dichotomicKeyboard.right();
  // TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());
}

void test_2_symbols_dichotomic_keyboard_should_navigate_to_right(void)
{
  Keys * alpha = new Keys("01", 2);
  Keys ** keys = new Keys*[1] {alpha};


  /* All of these should pass */
  DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('1', dichotomicKeyboard.currentLetter());


  dichotomicKeyboard.left();
  TEST_ASSERT_EQUAL('1', dichotomicKeyboard.currentLetter());
}

void test_2_symbols_dichotomic_keyboard_should_navigate_to_left(void)
{
  Keys * alpha = new Keys("01", 2);
  Keys ** keys = new Keys*[1] {alpha};


  /* All of these should pass */
  DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.left();
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());
}

void test_3_symbols_dichotomic_keyboard_should_navigate_to_right(void)
{
  Keys * alpha = new Keys("012", 3);
  Keys ** keys = new Keys*[1] {alpha};

  DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);
  TEST_ASSERT_EQUAL('1', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('2', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('2', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.left();
  TEST_ASSERT_EQUAL('2', dichotomicKeyboard.currentLetter());
}

void test_3_symbols_dichotomic_keyboard_should_navigate_to_left(void)
{
  Keys * alpha = new Keys("012", 3);
  Keys ** keys = new Keys*[1] {alpha};

  DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);
  TEST_ASSERT_EQUAL('1', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.left();
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.left();
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('0', dichotomicKeyboard.currentLetter());
}

void test_4_symbols_dichotomic_keyboard_should_navigate_to_right(void)
{
  Keys * alpha = new Keys("0123", 4);
  Keys ** keys = new Keys*[1] {alpha};

  /* All of these should pass */
  DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);
  TEST_ASSERT_EQUAL('1', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('2', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('3', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.left();
  TEST_ASSERT_EQUAL('3', dichotomicKeyboard.currentLetter());
}


void test_dichotomic_keyboard_should_navigate_to_right_or_left(void)
{
  Keys * alpha = new Keys("abcdef", 6);
  Keys ** keys = new Keys*[1] {alpha};

  DichotomicKeyboard dichotomicKeyboard = DichotomicKeyboard(keys, 1);
  TEST_ASSERT_EQUAL('c', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('e', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.left();
  TEST_ASSERT_EQUAL('d', dichotomicKeyboard.currentLetter());

  dichotomicKeyboard.right();
  TEST_ASSERT_EQUAL('d', dichotomicKeyboard.currentLetter());

}

void run() {
    UNITY_BEGIN();
    RUN_TEST(test_1_symbol_dichotomic_keyboard_should_always_give_same_letter);
    RUN_TEST(test_2_symbols_dichotomic_keyboard_should_navigate_to_right);
    RUN_TEST(test_2_symbols_dichotomic_keyboard_should_navigate_to_left);
    RUN_TEST(test_3_symbols_dichotomic_keyboard_should_navigate_to_right);
    RUN_TEST(test_3_symbols_dichotomic_keyboard_should_navigate_to_left);
    RUN_TEST(test_4_symbols_dichotomic_keyboard_should_navigate_to_right);
    RUN_TEST(test_4_symbols_dichotomic_keyboard_should_navigate_to_right);
    RUN_TEST(test_dichotomic_keyboard_should_navigate_to_right_or_left);
    UNITY_END();
}

#ifndef ARDUINO

int main(int argc, char **argv) {
   run();
}

#else

void setup() {
	run();
}

void loop() {
}

#endif
#endif
