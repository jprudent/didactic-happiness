(ns cryptopals.core-test
  (:require [clojure.test :refer :all]
            [cryptopals.core :refer :all]
            [cryptopals.ascii-bytes :refer :all]
            [cryptopals.xor :refer :all]))

(deftest set_1_1
  (testing "Base 64"
    (is (= "SSdtIGtpbGxpbmcgeW91ciBicmFpbiBsaWtlIGEgcG9pc29ub3VzIG11c2hyb29t"
           (bytes->base64 (hexstring->bytes "49276d206b696c6c696e6720796f757220627261696e206c696b65206120706f69736f6e6f7573206d757368726f6f6d"))))))

(deftest set_1_2
         (testing "Xor"
           (is (= (hexstring->bytes "746865206b696420646f6e277420706c6179")
                  (xor (hexstring->bytes "1c0111001f010100061a024b53535009181c")
                       (hexstring->bytes "686974207468652062756c6c277320657965"))))))
(deftest set_1_3
  (testing "single byte xor"
    (is (= "Cooking MC's like a pound of bacon"
           (ffirst (crack-single-byte-xor-key (hexstring->bytes "1b37373331363f78151b7f2b783431333d78397828372d363c78373e783a393b3736")))))))