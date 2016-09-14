(ns cryptopals.core-test
  (:require [clojure.test :refer :all]
            [cryptopals.core :refer :all]
            [cryptopals.ascii-bytes :refer :all]))

(deftest set_1_1
  (testing "Base 64"
    (is (= "SSdtIGtpbGxpbmcgeW91ciBicmFpbiBsaWtlIGEgcG9pc29ub3VzIG11c2hyb29t"
           (bytes->base64 (hexstring->bytes "49276d206b696c6c696e6720796f757220627261696e206c696b65206120706f69736f6e6f7573206d757368726f6f6d"))))))
