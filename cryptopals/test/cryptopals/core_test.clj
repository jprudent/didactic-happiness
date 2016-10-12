(ns cryptopals.core-test
  (:require [clojure.test :refer :all]
            [cryptopals.core :refer :all]
            [cryptopals.ascii-bytes :refer :all]
            [cryptopals.xor :refer :all]
            [cryptopals.aes :refer :all]
            [cryptopals.aes-detect :refer :all]))

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
           (-> (hexstring->bytes "1b37373331363f78151b7f2b783431333d78397828372d363c78373e783a393b3736")
               (crack-single-byte-xor-key)
               (ffirst)
               (bytes->ascii-string))))))

(deftest set_1_4
  (testing "find the ciphered block"
    (is (= "Now that the party is jumping\n"
           (->> (slurp "https://cryptopals.com/static/challenge-data/4.txt")
                (clojure.string/split-lines)
                (mapcat (comp crack-single-byte-xor-key hexstring->bytes))
                (sort by-second-descending)
                (ffirst)
                (bytes->ascii-string))))))

(deftest set_1_5
  (testing "implementing repeating XOR"
    (is (= "0b3637272a2b2e63622c2e69692a23693a2a3c6324202d623d63343c2a26226324272765272a282b2f20430a652e2c652a3124333a653e2b2027630c692b20283165286326302e27282f"
           (xor-text "Burning 'em, if you ain't quick and nimble\nI go crazy when I hear a cymbal"
                     "ICE")))))

(deftest set_1_6
  (testing "Hamming distance"
    (is (= 37
           (hamming-distance (ascii-string->bytes "this is a test")
                             (ascii-string->bytes "wokka wokka!!!")))))
  (testing "Cracking repeated xor key"
    (is (some (fn [s] (clojure.string/includes? s "Vanilla Ice is sellin' and you people are buyin'"))
              (->> (clojure.string/replace (slurp "https://cryptopals.com/static/challenge-data/6.txt") "\n" "")
                   (base64->bytes)
                   (crack-repeating-xor-key)
                   (map second)
                   (map bytes->ascii-string))))))

(deftest set_1_7
  (testing "Decipher an AES 128 in ECB mode"
    (is (clojure.string/includes?
          (-> (slurp "http://www.cryptopals.com/static/challenge-data/7.txt")
              (clojure.string/replace "\n" "")
              (base64->bytes)
              (decipher-ecb (ascii-string->bytes "YELLOW SUBMARINE"))
              (bytes->ascii-string))
          "Supercalafragilisticexpialidocious"))))

(deftest set_1_8
  (testing "Detect AES in ECB mode"
    (is (clojure.string/starts-with?
          (->> (slurp "http://www.cryptopals.com/static/challenge-data/8.txt")
               (clojure.string/split-lines)
               (map cryptopals.ascii-bytes/hexstring->bytes)
               (detect-aes-ecb)
               first
               bytes->hexstring)
          "d88061"))))

(deftest set_2_9
  (testing "Padding using PKCS#7"
    (is (= "YELLOW SUBMARINE\u0004\u0004\u0004\u0004"
           (-> (ascii-string->bytes "YELLOW SUBMARINE")
               (pkcs7-padding 20)
               (bytes->ascii-string))))
    (is (= (apply str "YELLOW SUBMARINE" (repeat 16 "\u0010"))
           (-> (ascii-string->bytes "YELLOW SUBMARINE")
               (pkcs7-padding 16)
               (bytes->ascii-string)))
        "An extra block is added if message is a multiple of block size")))

(deftest set_2_10
  (testing "Decipher AES 128 CBC"
    (is (clojure.string/includes?
          (-> (slurp "http://www.cryptopals.com/static/challenge-data/10.txt")
              (clojure.string/replace "\n" "")
              (cryptopals.ascii-bytes/base64->bytes)
              (decipher-cbc (ascii-string->bytes "YELLOW SUBMARINE") (repeat 16 0))
              (bytes->ascii-string))
          "The girlies sa y they love me and that is ok"))))

(deftest set_2_11
  (testing "Detecting ECB or CBC"
    (let [random-iv         random-key
          encryption-oracle (fn [plain-bytes]
                              (let [my-bytes (random-bytes (+ 5 (rand-int 6)))
                                    key      (random-key)
                                    cipher   (if (even? (rand-int 7))
                                               #(cipher-ecb % key)
                                               #(cipher-cbc % key (random-iv)))]
                                (-> (concat my-bytes plain-bytes my-bytes)
                                    cipher)))]
      (with-redefs [rand-int (constantly 0)]
        (is (ecb-mode? (encryption-oracle (repeat 333 0xAA)))))
      (with-redefs [rand-int (constantly 1)]
        (is (not (ecb-mode? (encryption-oracle (repeat 333 0xAA)))))))))

(deftest set_2_12-14
  (testing "Crack ECB mode (easy)"
    (let [key          (random-key)
          cipher-ecb   (fn [plain-bytes] (cipher-ecb plain-bytes key))
          unknown-text (base64->bytes "Um9sbGluJyBpbiBteSA1LjAKV2l0aCBteSByYWctdG9wIGRvd24gc28gbXkgaGFpciBjYW4gYmxvdwpUaGUgZ2lybGllcyBvbiBzdGFuZGJ5IHdhdmluZyBqdXN0IHRvIHNheSBoaQpEaWQgeW91IHN0b3A/IE5vLCBJIGp1c3QgZHJvdmUgYnkK")
          oracle       (fn [plain-bytes]
                         (cipher-ecb
                           (concat plain-bytes unknown-text)))]
      (is (= unknown-text
             (one-byte-at-a-time-attack oracle)))))
  (testing "Cracking ECB mode (hard)"
    (let [unknown-text (ascii-string->bytes "Simple made easy : Clojure")
          key          (random-key)
          prelude      (random-bytes (rand-int 123))
          oracle       (fn [plain-bytes]
                         (cipher-ecb (concat prelude
                                             plain-bytes
                                             unknown-text)
                                     key))]
      (is (= unknown-text
             (one-byte-at-a-time-attack oracle))))))

(deftest set_2_13
  (testing "copy paste attack"
    (let [query-string->map (fn [q] (->> (clojure.string/split q #"&")
                                         (map #(clojure.string/split % #"="))
                                         (into {})))
          map->query-string (fn [o] (clojure.string/join "&" (map (fn [[k v]] (str k "=" v)) o)))
          sanitize          (fn [s] (clojure.string/replace s #"&|=" ""))
          profile-for       (fn [email] (map->query-string
                                          (array-map "email" (sanitize email)
                                                     "uid" 10
                                                     "role" "user")))
          key               (random-key)
          encrypt           (fn [s] (cipher-ecb (cryptopals.ascii-bytes/ascii-string->bytes s)
                                                key))
          decrypt           (fn [bytes] (-> (decipher-ecb bytes key)
                                            bytes->ascii-string
                                            query-string->map))
          oracle            (comp encrypt profile-for)
          mk-string         (fn [size] (apply str (repeat size "X")))
          copy-paste-attack (fn [oracle]
                              (let [block-size    (detect-block-size oracle)
                                    forged-head   (take (* 2 block-size) (oracle (mk-string 13))) ;; forge a block like "...role="
                                    forged-middle (take block-size              ;; forge a block like "admin&...&rol"
                                                        (drop block-size
                                                              (oracle (str (mk-string 10) "admin"))))
                                    forged-tail   (take block-size
                                                        (drop (* 2 block-size)
                                                              (oracle (str (mk-string 14)))))] ;; forge a block like "=user..."
                                (concat forged-head forged-middle forged-tail)))]
      (is (= "admin" (-> (copy-paste-attack oracle) decrypt (get "role")))))))