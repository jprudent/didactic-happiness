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

unsigned long readMemoryAt(pid_t tracee, unsigned long address) {
  return ptrace(PTRACE_PEEKTEXT, tracee, address, NULL);
}

void writeMemoryAt(pid_t tracee, unsigned long address, unsigned long instruction) {
  ptrace(PTRACE_POKETEXT, tracee, address, instruction);
}

unsigned long readRegister(pid_t tracee, int reg) {
  return ptrace(PTRACE_PEEKUSER, tracee, 8 * reg, NULL);
}

void writeRegister(pid_t tracee, int reg, unsigned long value) {
  ptrace(PTRACE_POKEUSER, tracee, 8 * reg, value);
}

unsigned long setbp(pid_t tracee, unsigned long address) {
    unsigned long original = readMemoryAt(tracee, address);
    unsigned long sigtrap = (original & 0xFFFFFFFFFFFFFF00) | 0x00000000000000CC;
    writeMemoryAt(tracee, address, sigtrap);
    printf("Set breakpoint at %lx, new instruction is %lx instead of %lx\n",
          address, readMemoryAt(tracee, address), original);
    return original;
}

void removebp(pid_t tracee, unsigned long address, unsigned long original) {
  unsigned long previously = readMemoryAt(tracee, address);
  writeMemoryAt(tracee, address, original);
  printf("Unset breakpoint at %lx, new instruction is %lx, instead of %lx\n",
       address, readMemoryAt(tracee, address), previously);
}

void showregisters(pid_t tracee) {
  printf("RIP = %lx\nRAX = %lx\n",
        readRegister(tracee, RIP), readRegister(tracee, ORIG_RAX));
}

void setIp(pid_t tracee, unsigned long address) {
  writeRegister(tracee, RIP, address);
}

void presskey() {
  getchar();
}

int main(int argc, char ** argv) {
    setbuf(stdout, NULL);
    unsigned long bpAddress = to_ulong(argv[1]);
    pid_t child = fork();
    if(child == 0) {
        ptrace(PTRACE_TRACEME, 0, NULL, NULL);
        execve(argv[2], argv + 2, NULL);
    }
    else {
        // wait for the child to stop
        waitchild(child);

        unsigned long originalInstruction = setbp(child, bpAddress);
        ptrace(PTRACE_CONT, child, NULL, NULL);

        while(waitchild(child) < 1) {
          printf("Breakpoint hit !\n");
          showregisters(child);
          presskey();

          removebp(child, bpAddress, originalInstruction);
          setIp(child, bpAddress);

          ptrace(PTRACE_SINGLESTEP, child, NULL, NULL);
          waitchild(child);

          setbp(child, bpAddress);

          ptrace(PTRACE_CONT, child, NULL, NULL);
        }
    }
    return 0;
}
