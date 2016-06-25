#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
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

void trace(pid_t child) {
  unsigned long instruction, opcode1, opcode2, ip;
  unsigned long jmps = 0;
  do {
    ip = ptrace(PTRACE_PEEKUSER, child, 8 * RIP, NULL);
    instruction = ptrace(PTRACE_PEEKTEXT, child, ip, NULL);
    opcode1 = instruction & 0x00000000000000FF;
    opcode2 = (instruction & 0x000000000000FF00) >> 8;
    if((opcode1 >= 0x70 && opcode1 <= 0x7F) ||
       (opcode1 == 0x0F && (opcode2 >= 0x83 && opcode2 <= 0x87))) {
         jmps = jmps + 1;
    }
    ptrace(PTRACE_SINGLESTEP, child, NULL, NULL);
  } while(waitchild(child) < 1);
  printf("\n=> There are %lu jumps\n", jmps);
}

int main(int argc, char ** argv) {
    long instruction;
    pid_t child = fork();
    if(child == 0) {
        ptrace(PTRACE_TRACEME, 0, NULL, NULL);
        execve(argv[1], argv + 1, NULL);
    }
    else {
        // wait for the child to stop
        waitchild(child);
        trace(child);
    }
    return 0;
}
