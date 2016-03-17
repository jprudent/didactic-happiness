#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <sys/user.h>
#include <sys/reg.h>

void fizzbuzz() {
    for(int i = 0; i < 100; i++) {
        int fizz = i % 3 == 0;
        if(fizz) printf("Fizz");
        int buzz = i % 5 == 0;
        if(buzz) printf("Buzz");
        if(!(fizz||buzz)) printf("%d", i);
        printf(", ");
        fflush(stdout);
    }
}

int main() {
    fizzbuzz();
}
