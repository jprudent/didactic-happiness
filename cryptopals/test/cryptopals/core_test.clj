(ns cryptopals.core-test
  (:require [clojure.test :refer :all]
            [cryptopals.core :refer :all]
            [cryptopals.ascii-bytes :refer :all]
            [cryptopals.xor :refer :all]))

(deftest set_1_1
  (testing "Encode to Base 64"
    (is (= "SSdtIGtpbGxpbmcgeW91ciBicmFpbiBsaWtlIGEgcG9pc29ub3VzIG11c2hyb29t"
           (bytes->base64 (hexstring->bytes "49276d206b696c6c696e6720796f757220627261696e206c696b65206120706f69736f6e6f7573206d757368726f6f6d"))))
    (is (= "" (bytes->base64 [])))
    (is (= "Zg==" (bytes->base64 (ascii-string->bytes "f"))))
    (is (= "Zm8=" (bytes->base64 (ascii-string->bytes "fo")))))
  (testing "Decode Base 64"
    (is (= "" (bytes->ascii-string (base64->bytes ""))))
    (is (= "f" (bytes->ascii-string (base64->bytes "Zg=="))))
    (is (= "fo" (bytes->ascii-string (base64->bytes "Zm8="))))))

(deftest set_1_2
  (testing "Xor"
    (is (= (hexstring->bytes "746865206b696420646f6e277420706c6179")
           (xor (hexstring->bytes "1c0111001f010100061a024b53535009181c")
                (hexstring->bytes "686974207468652062756c6c277320657965"))))))
(deftest set_1_3
  (testing "single byte xor"
    (is (= "Cooking MC's like a pound of bacon"
           (ffirst (crack-single-byte-xor-key (hexstring->bytes "1b37373331363f78151b7f2b783431333d78397828372d363c78373e783a393b3736")))))))

(deftest set_1_4
  (testing "find the ciphered block"
    (is (= "Now that the party is jumping\n"
           (->> (slurp "https://cryptopals.com/static/challenge-data/4.txt")
                (clojure.string/split-lines)
                (mapcat (comp crack-single-byte-xor-key hexstring->bytes))
                (sort by-second-descending)
                (ffirst))))))

(deftest set_1_5
  (testing "implementing repeating XOR"
    (is (= "0b3637272a2b2e63622c2e69692a23693a2a3c6324202d623d63343c2a26226324272765272a282b2f20430a652e2c652a3124333a653e2b2027630c692b20283165286326302e27282f"
           (xor-text "Burning 'em, if you ain't quick and nimble\nI go crazy when I hear a cymbal"
                     "ICE")))))

(deftest set_1_6
  (testing "Hamming distance"
    (is (= 37
           (hamming-distance (ascii-string->bytes "this is a test")
                             (ascii-string->bytes "wokka wokka!!!"))))))