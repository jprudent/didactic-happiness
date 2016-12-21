(ns cryptopals.aes-test
  (:require [clojure.test :refer :all]
            [cryptopals.aes :refer :all]
            [cryptopals.ascii-bytes :refer :all]))

(deftest test_gf*
  (testing "(x^6 + x^4 + x^2 + x + 1)(x^7 + x + 1) = x^7 + x^6 + 1"
    (is (= 2r11000001 (gf* 2r1010111 2r10000011))))
  (testing "(x^6 + x^4 + x + 1) and (x^7 + x^6 + x^3 + x) are multiplicative inverse"
    (is (= 2r1 (gf* 2r1010011 2r11001010)))))

(deftest byte_rotate
  (testing "byte rotation"
    (is (= 2r10000001 (cycle-bits-right 2r11)))))

(deftest sub_byte
  (testing "SubByte operation"
    (is (= [0x63 0x77 0x85 0xc1 0x16]
           (map s-box [0x00 0x02 0x67 0xdd 0xff])))))

(deftest key_expansion
  (testing "Key expansion"
    (let [key (key-expansion [0x2b 0x7e 0x15 0x16
                              0x28 0xae 0xd2 0xa6
                              0xab 0xf7 0x15 0x88
                              0x09 0xcf 0x4f 0x3c])]
      (is (= [0x3D 0x80 0x47 0x7D] (nth key 12)))
      (is (= [0x47 0x16 0xFE 0x3E] (nth key 13)))
      (is (= [0x1E 0x23 0x7E 0x44] (nth key 14)))
      (is (= [0x6D 0x7A 0x88 0x3B] (nth key 15)))
      (is (= [0xB6 0x63 0x0C 0xA6] (nth key 43))))))

(deftest mix_columns
  (testing "MixColumns operation"
    (is (= [[0x04 0xe0 0x48 0x28]
            [0x66 0xcb 0xf8 0x06]
            [0x81 0x19 0xd3 0x26]
            [0xe5 0x9a 0x7a 0x4c]]
           (mix-columns
             [[0xd4 0xe0 0xb8 0x1e]
              [0xbf 0xb4 0x41 0x27]
              [0x5d 0x52 0x11 0x98]
              [0x30 0xae 0xf1 0xe5]])))))

(deftest cipher_block
  (testing "Cipher a block (example taken from appendix B of FIPS 197)"
    (is (= [[0x39 0x02 0xdc 0x19]
            [0x25 0xdc 0x11 0x6a]
            [0x84 0x09 0x85 0x0b]
            [0x1d 0xfb 0x97 0x32]]
           (cipher-block
             [[0x32 0x88 0x31 0xE0]
              [0x43 0x5a 0x31 0x37]
              [0xF6 0x30 0x98 0x07]
              [0xA8 0x8D 0xA2 0x34]]
             [0x2b 0x7e 0x15 0x16
              0x28 0xae 0xd2 0xa6
              0xab 0xf7 0x15 0x88
              0x09 0xcf 0x4f 0x3c]
             )))))

(deftest decipher_block
  (testing "Decipher a block"
    (is (= [[0x32 0x88 0x31 0xE0]
            [0x43 0x5a 0x31 0x37]
            [0xF6 0x30 0x98 0x07]
            [0xA8 0x8D 0xA2 0x34]]
           (decipher-block
             [[0x39 0x02 0xdc 0x19]
              [0x25 0xdc 0x11 0x6a]
              [0x84 0x09 0x85 0x0b]
              [0x1d 0xfb 0x97 0x32]]
             [0x2b 0x7e 0x15 0x16
              0x28 0xae 0xd2 0xa6
              0xab 0xf7 0x15 0x88
              0x09 0xcf 0x4f 0x3c])))))

(deftest decipher_cipher
  (testing "The almighty symmetry of operations"                                ;; TODO property based testing
    (let [block [[0x32 0x88 0x31 0xE0]
                 [0x43 0x5a 0x31 0x37]
                 [0xF6 0x30 0x98 0x07]
                 [0xA8 0x8D 0xA2 0x34]]
          key   [0x2b 0x7e 0x15 0x16
                 0x28 0xae 0xd2 0xa6
                 0xab 0xf7 0x15 0x88
                 0x09 0xcf 0x4f 0x3c]]
      (is (= block (decipher-block (cipher-block block key) key))))))

(deftest decipher_cbc
  (testing "One block"                                                          ;; see https://tools.ietf.org/html/rfc3602
    (is (= "Single block msg"
           (-> (decipher-cbc
                 (hexstring->bytes "e353779c1079aeb82708942dbe77181a")
                 (hexstring->bytes "06a9214036b8a15b512e03d534120006")
                 (hexstring->bytes "3dafba429d9eb430b422da802c9fac41"))
               (bytes->ascii-string))))
    (is (= (hexstring->bytes "e353779c1079aeb82708942dbe77181a")
           (-> (cipher-cbc
                 (ascii-string->bytes "Single block msg")
                 (hexstring->bytes "06a9214036b8a15b512e03d534120006")
                 (hexstring->bytes "3dafba429d9eb430b422da802c9fac41")))))))