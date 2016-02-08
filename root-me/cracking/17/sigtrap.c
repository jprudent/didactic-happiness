#include <stdio.h>
#include <unistd.h>
#include <sys/syscall.h>

void h1(int a) {
  printf("h1 %d", a);
}

int main(int arc, char ** argv){
  syscall(0x30, 0x5, h1);
  __asm__("int3");
  printf("bye");
}

