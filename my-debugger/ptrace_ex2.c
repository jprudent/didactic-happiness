#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <sys/user.h>
#include <sys/reg.h>

void waitchild(pid_t pid) {
    int status;
    waitpid(pid, &status, 0); 
    if(WIFSTOPPED(status)) {
        printf("%d stopped with signal %d\n", pid, WSTOPSIG(status));
    }
    else if (WIFEXITED(status)) {
        printf("%d exited with status %d\n", pid, WEXITSTATUS(status));
    } 
    else {
        printf("%d raised an unexpected status %d", pid, status);
    }
}

int main() {
    long orig_rax;
    pid_t child = fork();
    if(child == 0) {
        ptrace(PTRACE_TRACEME, 0, NULL, NULL);
        child = getpid();
        kill(child, SIGUSR1);
        printf("Hasta luego\n");
    }
    else {
        // wait for the child to stop
        waitchild(child); 
        orig_rax = ptrace(PTRACE_PEEKUSER,
                          child, 8 * ORIG_RAX,
                          NULL);
        printf("The child made a "
               "system call %ld\n", orig_rax);
        ptrace(PTRACE_CONT, child, NULL, NULL);
        // wait for the child to exit
        waitchild(child);
    }
    return 0;
}

