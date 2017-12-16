# repicene

You like GameBoy ? Me too :)

## Development

### Configure the REPL

Printing the CPU generates a lot of output. This will limit the output to 10 elements in the output. 

    (set! *print-length* 10)

## Clojure pitfalls

Use `nth` rather than `get` for getting a value in a vector. The day your collection won't be a vector, `get` won't work.

The repl is not a substitute for unit testing (but sometimes feels like it does, I'm lost)

Overloaded functions get an overhead !!!

## Performance

b16b392321c0f6883f4a0a4695aa1e8edf60d8ae
Evaluation count : 60 in 60 samples of 1 calls.
             Execution time mean : 2.537500 sec
    Execution time std-deviation : 61.409875 ms
   Execution time lower quantile : 2.408440 sec ( 2.5%)
   Execution time upper quantile : 2.630590 sec (97.5%)
                   Overhead used : 1.968864 ns


## TODO 

- Performance task force :
    - inline registers in cpu map
        - they can ultimately be stored in a single 64bits integer
        

- all address arithmetic should be done modulo 0xFFFF
- double check instruction size
- don't neeed to destructure record field



