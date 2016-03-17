#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <signal.h>
#include <sys/user.h>
#include <sys/reg.h>

int waitchild(pid_t pid) {
    int status;
    waitpid(pid, &status, 0);
    if(WIFSTOPPED(status)) {
        return 0;
    }
    else if (WIFEXITED(status)) {
        return 1;
    }
    else {
        printf("%d raised an unexpected status %d", pid, status);
        return 1;
    }
}

unsigned long to_ulong(char * s) {
  return strtol(s, NULL, 16);
}

unsigned long readRegister(pid_t tracee, int reg) {
  return ptrace(PTRACE_PEEKUSER, tracee, 8 * reg, NULL);
}

void showregisters(pid_t tracee) {
  printf("RIP = %lx\n",
        readRegister(tracee, RIP));
}

void presskey() {
  getchar();
}

int main(int argc, char ** argv) {
    unsigned long bpAddress = to_ulong(argv[1]);
    pid_t child = fork();
    unsigned long rip;
    if(child == 0) {
        ptrace(PTRACE_TRACEME, 0, NULL, NULL);
        execve(argv[2], argv + 2, NULL);
    }
    else {
        // wait for the child to stop
        waitchild(child);
        do {
          rip = readRegister(child, RIP);
          if(rip == bpAddress) {
            showregisters(child);
            presskey();
          }
          ptrace(PTRACE_SINGLESTEP, child, NULL, NULL);
        } while(waitchild(child) < 1);
    }
    return 0;
}
