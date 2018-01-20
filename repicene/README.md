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

Use `nth` rather than `get` for getting a value in a vector. The day your collection won't be a vector, `get` won't work.

The repl is not a substitute for unit testing (but sometimes feels like it does, I'm lost)

Hey I'm Jerome 6 months in the future! I little bastard didn't write enough tests.

Overloaded functions get an overhead !!!

get-in is slower than ->

native arrays are faster than transient vectors





## TODO 

- Performance task force :
    - inline registers in cpu map.
      They can ultimately be stored in a single 64bits integer
    - do records are faster than maps?
        

- all address arithmetic should be done modulo 0xFFFF
- double check instruction size



