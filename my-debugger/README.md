This is a toy used to learn what a debugger is made of.

# On the web 

- Playing with `ptrace` [http://www.linuxjournal.com/article/6100](Part I), [http://www.linuxjournal.com/article/6210](Part II)

- Nice [https://mikecvet.wordpress.com/2010/08/14/ptrace-tutorial/](write up about ptrace)

- Why [http://lwn.net/Articles/371501/](ptrace is aweful) ?


J'utilise du franglish.
Je ne parle que de Linux sous archi x86-64

## Qu'est-ce que le CPU
- registres

## Qu'est-ce que le kernel ?
priviledged 

## Qu'est qu'un process ?
non priviledged
- accède qu'à certaines parties de la mémoire

Un process est la version vivante d'un programme.
Constitué de :
 - data
 - code machine (text) asm

## Qu'est-ce qu'un process signal ?

Permet d'envoyer un signal à un process en cours.
Un process enregistre un handler pour chaque type de signal.

## Qu'est-ce qu'une software interruption ou trap ou exception ?
Une interruption est un signal envoyé au kernel depuis un process.
Chaque type d'interruption est associé à un handler.
exemple:
 - exit
 - syscall

## Qu'est-ce qu'un system call ?

Utilisé quand un process a besoin de faire ce que seul le noyau a le droit de faire.
C'est une API.
Sous Linux, elle implément le standard POSIX.
Matérialisé sous la forme d'une interruption (0x80)

exemple :
 - I/O
 - process management 

[http://blog.rchapman.org/post/36801038863/linux-system-call-table-for-x86-64](Linux System Call Table for x86_64)

## ptrace

```
#include <stdio.h>

int main() { 
  printf("hello");
  return 0; 
}
```

gcc -o toto toto.c

./toto.c
hello

```
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <stdio.h>
#include <sys/user.h>
#include <sys/reg.h>

int main()
{   pid_t child;
    long orig_rax;
    child = fork();
    if(child == 0) {
        ptrace(PTRACE_TRACEME, 0, NULL, NULL);
        execl("toto", "toto", NULL);
    }
    else {
        wait(NULL);
        orig_rax = ptrace(PTRACE_PEEKUSER,
                          child, 8 * ORIG_RAX,
                          NULL);
        printf("The child made a "
               "system call %ld\n", orig_rax);
        ptrace(PTRACE_CONT, child, NULL, NULL);
    }
    return 0;
}
```

gcc -o ptrace_ex1 ptrace_ex1.c
./ptrace_ex1                  
The child made a system call 59

