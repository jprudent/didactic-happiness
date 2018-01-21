# repicene

You like GameBoy ? Me too :)

## Development

### Configure the REPL

Printing the CPU generates a lot of output. This will limit the output to 10 elements in the output. 

    (set! *print-length* 10)

## Performance

In dev mode, assertions are activated. A lot 
of functions have pre/post conditions.

Difference between assertion activated or not 
are 3 levels of magnitude 

with `*assert*` true :

    Evaluation count : 60 in 60 samples of 1 calls.
                 Execution time mean : 2.537500 sec
        Execution time std-deviation : 61.409875 ms
       Execution time lower quantile : 2.408440 sec ( 2.5%)
       Execution time upper quantile : 2.630590 sec (97.5%)
                       Overhead used : 1.968864 ns

with `*assert*` false 

    Evaluation count : 1068120 in 60 samples of 17802 calls.
                 Execution time mean : 49.949607 µs
        Execution time std-deviation : 5.216644 µs
       Execution time lower quantile : 41.826417 µs ( 2.5%)
       Execution time upper quantile : 57.192068 µs (97.5%)
                       Overhead used : 3.121585 ns
                   
So, in dev mode, if you need performance :

```clojure
(require '[clojure.tools.namespace.repl :refer [refresh refresh-all]])
(set! *assert* false)
(refresh-all)
```

### Breakpoints implementation

Breakpoints are implemented as instructions. When a breakpoint
 is set, a breakpoint instruction is written at memory location
  and old opcode is backuped. 
When breakpoint instruction is executed, 
it restores the original instruction, then
it passes the state of the CPU
to debug mode and debug commands can be received.


## Clojure pitfalls

#### `nth` or `get` ?
Use `nth` rather than `get` for getting a value in a vector. The day your collection won't be a vector, `get` won't work.

#### repl or unit test ?
The repl is not a substitute for unit testing (but sometimes feels like it does, I'm lost)

Hey I'm Jerome 6 months in the future! I little bastard didn't write enough tests.

#### Overloaded functions 
Overloaded functions get an overhead !!!

#### `get-in` is slow
get-in is slower than ->

#### native or transient ?
native arrays are faster than transient vectors

#### Map or Record ?

Nested read is 2 times faster
```
(criterium/bench (let [r {:v {:b 1}}]
  (+ (-> r :v :b) (-> r :v :b))))
Evaluation count : 947740740 in 60 samples of 15795679 calls.
             Execution time mean : 63.884901 ns
    Execution time std-deviation : 1.819705 ns
   Execution time lower quantile : 60.538128 ns ( 2.5%)
   Execution time upper quantile : 67.060168 ns (97.5%)
                   Overhead used : 2.263128 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 15.7751 % Variance is moderately inflated by outliers
=> nil
```

```
(criterium/bench (let [r (->R (->S 1))]
  (+ (-> r :v :b) (-> r :v :b))))
Evaluation count : 1789626720 in 60 samples of 29827112 calls.
             Execution time mean : 32.909643 ns
    Execution time std-deviation : 1.194557 ns
   Execution time lower quantile : 31.148209 ns ( 2.5%)
   Execution time upper quantile : 35.455290 ns (97.5%)
                   Overhead used : 2.263128 ns

Found 3 outliers in 60 samples (5.0000 %)
	low-severe	 3 (5.0000 %)
 Variance from outliers : 22.2572 % Variance is moderately inflated by outliers
=> nil
```

Nested update with update-in is always slow

```
(let [r (->R (->S 1))]
  (update-in r [:v :b] inc))
=> #repicene.test_rom_suite.R{:v #repicene.test_rom_suite.S{:b 2}}
(criterium/quick-bench (let [r (->R (->S 1))]
                         (update-in r [:v :b] inc)))
Evaluation count : 1119066 in 6 samples of 186511 calls.
             Execution time mean : 552.938684 ns
    Execution time std-deviation : 42.781889 ns
   Execution time lower quantile : 504.424366 ns ( 2.5%)
   Execution time upper quantile : 608.790912 ns (97.5%)
                   Overhead used : 2.263128 ns
=> nil
(criterium/quick-bench (let [r {:v {:b 1}}]
                         (update-in r [:v :b] inc)))
Evaluation count : 1361376 in 6 samples of 226896 calls.
             Execution time mean : 477.159396 ns
    Execution time std-deviation : 47.103246 ns
   Execution time lower quantile : 389.753900 ns ( 2.5%)
   Execution time upper quantile : 520.123982 ns (97.5%)
                   Overhead used : 2.263128 ns

Found 1 outliers in 6 samples (16.6667 %)
	low-severe	 1 (16.6667 %)
 Variance from outliers : 30.3641 % Variance is moderately inflated by outliers
=> nil
```

Nested update with update is faster than update-in, both solution are 
more or less equivalent

```
(criterium/quick-bench (let [r (->R (->S 1))]
                         (update r :v update :b inc )))
Evaluation count : 3185154 in 6 samples of 530859 calls.
             Execution time mean : 206.916790 ns
    Execution time std-deviation : 19.396670 ns
   Execution time lower quantile : 182.877713 ns ( 2.5%)
   Execution time upper quantile : 227.016983 ns (97.5%)
                   Overhead used : 2.263128 ns
=> nil
(criterium/quick-bench (let [r {:v {:b 1}}]
                         (update r :v update :b inc)))
Evaluation count : 2737296 in 6 samples of 456216 calls.
             Execution time mean : 228.517142 ns
    Execution time std-deviation : 12.634164 ns
   Execution time lower quantile : 211.835587 ns ( 2.5%)
   Execution time upper quantile : 239.270171 ns (97.5%)
                   Overhead used : 2.263128 ns
```

Simple update is 2x faster

```
(criterium/quick-bench (let [r {:v {:b 1}}]
                         (update r :v count)))
Evaluation count : 3719778 in 6 samples of 619963 calls.
             Execution time mean : 177.849686 ns
    Execution time std-deviation : 14.241716 ns
   Execution time lower quantile : 161.799128 ns ( 2.5%)
   Execution time upper quantile : 194.793146 ns (97.5%)
                   Overhead used : 2.263128 ns
=> nil
(criterium/quick-bench (let [r (->R (->S 1))]
                         (update r :v count )))
Evaluation count : 9983316 in 6 samples of 1663886 calls.
             Execution time mean : 67.158613 ns
    Execution time std-deviation : 7.531323 ns
   Execution time lower quantile : 58.558638 ns ( 2.5%)
   Execution time upper quantile : 76.549121 ns (97.5%)
                   Overhead used : 2.263128 ns
=> ni
```

#### Use protocols and records

Use them early. It helps to structure your program. It helps with
SOLID principles. They are faster than pure data structures.


## TODO 

- Performance task force :
    - inline registers in cpu map.
      They can ultimately be stored in a single 64bits integer
    - do records are faster than maps?
        

- all address arithmetic should be done modulo 0xFFFF
- double check instruction size

## Tracklist

Music that helped :

- Stephan Bodzin & Marc Romboy - Kerberos 

