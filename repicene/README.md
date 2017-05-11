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

## TODO 

- Performance task force :
    - inline registers in cpu map
        - they can ultimately be stored in a single 64bits integer
        

- all address arithmetic should be done modulo 0xFFFF
- double check instruction size
- don't neeed to destructure record field



