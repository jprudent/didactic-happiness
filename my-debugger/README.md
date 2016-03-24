This is a toy used to learn what a debugger is made of.

# Références 

- Playing with `ptrace` [http://www.linuxjournal.com/article/6100](Part I), [http://www.linuxjournal.com/article/6210](Part II)

- Nice [https://mikecvet.wordpress.com/2010/08/14/ptrace-tutorial/](write up about ptrace)

- Why [http://lwn.net/Articles/371501/](ptrace is aweful) ?

- man 2 ptrace

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

exemple :
 - I/O
 - process management 

[http://blog.rchapman.org/post/36801038863/linux-system-call-table-for-x86-64](Linux System Call Table for x86_64)

Chaque syscall a un identifiant. Il peut avoir jusqu'à 6 paramètres passés par registre CPU.

Exemple: Affichage à l'écran (copié d'[ici](http://cs.lmu.edu/~ray/notes/linuxsyscalls))
Voici ce que donne le code désassemblé d'un hello world (`objdump -d a.out`):

```
  4000d4:	48 c7 c0 01 00 00 00 	mov    $0x1,%rax
  4000db:	48 c7 c7 01 00 00 00 	mov    $0x1,%rdi
  4000e2:	48 c7 c6 fe 00 40 00 	mov    $0x4000fe,%rsi
  4000e9:	48 c7 c2 0d 00 00 00 	mov    $0xd,%rdx
  4000f0:	0f 05                	syscall 
```

Traduit en français, cela donne : "Appel du syscall `sys_write` (RAX=1) pour écrire dans le file descriptor 1 (RDI=1), alias la sortie standard, la chaîne de caractère à l'adresse 0x4000fe (RSI=0x4000fe) de longueur 13 (RDX=0xd). Notez l'instruction `syscall` qui est une vraie instruction assembleur. 
## ptrace

```
#include <stdio.h>

int main() { 
  printf("hello");
  return 0; 
}
```

gcc -o toto toto.c

./toto
hello

## Première implémentation d'un debugger

Dans cette première étape, nous allons voir comment démarrer un processus en "mode debug", le mettre en pause, l'inspecter et le laisser reprendre son exécution.

Je tiens à préciser que cette partie est une adaptation 64 bits de [cet article original](http://www.linuxjournal.com/article/6100?page=0,0). N'hésitez pas à vous y référer si mes explications sont insuffisantes :)

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

L22: clone du process. Pour rappel, le syscall `fork` est le seul moyen de créer des process (à ma connaissance). `fork` procède à une copie presque intégrale du processus appelant (mémoire, registres CPU, ...). L'appelant devient le processus père du clone qui est donc son fils. Les 2 processus continuent leurs exécution aprè l'appel à `fork`. Je disais copie _presque_ intégrale car dans le processus père `fork` renvoie le PID du fils et dans le fils il renvoie 0. Dans notre exemple le père est le _debugger_ et le fils le _tracee_.

Tracee :

L14: se mettre en mode TRACEME. Dans ce mode, le process enfant s'arrête à chaque fois qu'il reçoit un signal.

L15: Remplacement du core image par celle de `toto` en faisant appel au syscall `execve` via la fonction `execl`. `fork` permet de créer des process, `execve` permet de les remplacer ! Dans la man page, on peut lire qu'un process en mode TRACEME reçoit implicitement un signal SIGTRAP quand il fait un appel à `execve`. Le tracee est donc arrêté juste avant qu'il n'ait pu exécuter `execve`.

Debugger :

L18: `wait` permet d'attendre un changement d'état dans l'un des processus fils. Nous avons vu que le tracee stoppe son exécution à l'appel de `execve`. Le debugger attend que le tracee passe à l'état STOPPED.  

L19: Le tracee étant arrêté, le débugger a tout le loisir de l'inspecter. Ici il utilise la commande `PTRACE_PEEKUSER` qui permet d'inspecter les registres du CPU, plus particulièrement le registre RAX. A l'issu de l'appel `ptrace`, la variable `orig_rax` contient ne numéro du syscall correspondant à `execve`, soit 59.
Les valeurs de registre ne sont pas lues en live depuis le CPU. En fait, quand le kernel stoppe le tracee il enregistre le contexte du processus, dont les registres, afin que ce dernier puisse reprendre son exécution plus tard, comme si de rien n'était. Les valeurs renvoyées par `ptrace` sont issues de cet enregistrement.

L24: Le debugger lance la commande PTRACE_CONT qui informe le kernel de laisser le tracee continuer son exécution. 


L26: Le debugger et le tracee terminent leur exécution.
 
Le man de ptrace liste toutes les commandes disponibles, parmis lesquelles la lecture et l'écriture des registres CPU ou de la mémoire du tracee. Indispensable pour écrire un débugger !


