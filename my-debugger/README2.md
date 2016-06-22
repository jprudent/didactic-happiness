# Motivations

Un debugger est un outil fabuleux :
Cette sensation de contrôle divin ! La possibilité de figer l'exécution d'un
process et d'inspecter les arcanes de sa mémoire.

C'était les deux phrases lyriques de cet article :) Nous verrons que le divin
n'est qu'une machinerie bien huilée.

Le débugger est un outil que j'utilise quotidiennement. Je trouve important
d'en comprendre les mécanismes sous-jacents. Ecrire un concurrent à
[GDB](https://en.wikipedia.org/wiki/GNU_Debugger) n'est certainement
pas la meilleur façon d'utiliser son temps libre. En revanche écrire un
[POC](https://en.wikipedia.org/wiki/Proof_of_concept) de débugger est certainement
la manière la plus didactique d'apprendre ! Et c'est ce que je vous propose
aujourd'hui, d'écrire un petit debugger pas super pratique mais fonctionnel.

Concernant le fond, cet article ne traite que de Linux sous architecture x86_64.

Concernant la forme, cet article est en franglish (parce que je trouve bizzare
d'écrire _deboggueur_).

# Rapide rappel sur le code assembleur

Si vous savez lire le code assembleur, vous pouvez sauter cette section.

- mnémonique / opcode
- registres CPU

# Rapide rappel sur les syscalls

Si vous savez déjà ce qu'est un syscall, vous pouvez sauter cette section.

Le processeur a plusieurs niveaux d'exécution :

- Typiquement le noyau Linux tourne dans un mode dit _privilégié_. Dans ce mode
il peut accèder à toute la mémoire, lire et écrire sur disque, ...

- Les autres process, comme votre navigateur, tournent dans un mode
_non privilégié_. Ils n'ont accès qu'à une certaine partie de la mémoire et ne
peuvent pas écrire directement sur disque.

Tant qu'un process se contente de faire des calculs et de lire et écrire en
mémoire, il est autonome. Mais dès qu'il décide d'agir sur son environnement
(a.k.a. _side effect_), comme écrire sur le disque, il doit utiliser un
_appel système_ a.k.a _syscall_.

Le process effectuant un _syscall_ donne la main au noyau et *bloque*
jusqu'à ce que le _syscall_ ait été effectué. Un _syscall_ est en général une
opération coûteuse en temps.

Linux implémente le standard [POSIX](https://en.wikipedia.org/wiki/POSIX)
qui définit un ensemble d'appels système.

En voici un extrait :

| %rax        | syscall           | %rdi                          | %rsi            | %rdx     | %rcx  | %r8 | %r9 |
| ----------- |:-------------:| -----:|
| 0           | read              | `unsigned int file_descriptor`| `char * buffer` | `size_t length`  |
| 1           | write             | `unsigned int file_descriptor`| `char * buffer` | `size_t length`  |
| 57          | fork              |
| 59          | execve            | `const char *filename`        | `const char *const argv[]` | `const char *const envp[]` |
| 60          | exit              | `int error_code` |
| 62          | kill              | `pid_t pid` | `int signal` |
| 101         | ptrace            | `long request`                | `long pid`      | `unsigned long data` |

 Allez voir la [liste complète](http://blog.rchapman.org/post/36801038863/linux-system-call-table-for-x86-64).

Chaque _syscall_ a un identifiant qui est placé dans le registre `RAX` et peut
avoir jusqu'à 6 paramètres passés par convention dans les registres
`RDI`, `RSI`, `RDX`, `RCX`, `R8`, `R9`.

`read` et `write` permettent de lire et d'écrire dans un fichier. Nous aborderons
les autres un peu plus tard.

L'exemple suivant est un typique "hello world" qui illustre un appel au
 _syscall_ `write` :

``` asm
mov    $0x1,%rax
mov    $0x1,%rdi
mov    $0x4000fe,%rsi
mov    $0xd,%rdx
syscall
```

Traduit en français, cela donne : "Appel du syscall `sys_write` (RAX=1) pour écrire dans le file descriptor 1 (RDI=1), alias la sortie standard, la chaîne de caractère à l'adresse 0x4000fe (RSI=0x4000fe) de longueur 13 (RDX=0xd). Notez l'instruction `syscall` qui est une vraie instruction assembleur x86_64.

Il existe un utilitaire très pratique, `strace`, qui permet de tracer tous les
_syscall_ effectués par un process.
Par exemple pour tracer tous les _syscall_ `write` de la commande `echo` :

    $strace -o '| grep write' echo "Hello"
    write(1, "Hello\n", 6)                  = 6

On voit que `write` a été appelé avec les paramètres :
- On écrit dans le file descriptor 1 (sortie standard),
- Une chaine de caractère qui contient "Hello\n"
- On écrit 6 caractères
- `write` a bien écrit 6 caractères

# Salut fiston, c'est papa !

Si vous savez déjà ce qu'est un `fork`, vous pouvez sauter cette section.

On peut imaginer qu'un debugger a une certaine emprise sur le process déboggé.
Sous Linux, ce genre d'abus de position s'exprime par une relation père fils.

Si vous avez une console à proximité et que vous tapez `pstree` vous
remarquerez que les process sont organisés hiérarchiquement. La racine commune à tous
est `systemd` (ou `init` sur des systèmes plus anciens) et votre navigateur
est une feuille de l'arbre.

Pour créer un process fils, un futur père utilise le syscall `fork`. C'est
d'ailleurs la seule façon possible de créer des process. Voici un code typique :

``` C
int main()
{   pid_t child = fork();
    if(child == 0) {
        printf("I am the child")
    }
    else {
        printf("I am the father of %d", child);
    }
    return 0;
}
```

`fork` procède à une copie presque intégrale du processus appelant (mémoire, registres CPU, ...). L'appelant devient le processus père du clone qui est donc son fils.
Quand `fork` rend la main, les 2 processus continuent leurs exécution juste après l'appel à `fork`, sur le `if`.

Je disais copie _presque_ intégrale car dans le processus père `fork` renvoie le PID du fils et dans le fils il renvoie 0. Le fils affichera donc "I am the child" et le père
"I am the father of 1234".

En extrapolant, on peut voir le `fork` comme une [
mitose](https://fr.wikipedia.org/wiki/Mitose) cellulaire. Avant la mitose
on a 1 cellule et après la mitose on a 2 cellules qui partagent exactement
le même ADN (le code).

# Signaux

TODO

# Trace moi si tu peux

Linux fournit un syscall appelé `ptrace` qui permet d'implémenter un
débugger.

Daurénavant nous parlerons de _tracer_ (le débugger) et de _tracee_ (le process
  à débugger), c'est le vocabulaire employé dans la page de `man` de `ptrace`.

Le _tracee_ fait appel à la commande `TRACEME` pour signaler qu'il souhaite
être tracé par son père. Dans ce mode, le process peut
être dans deux états possibles. Soit il est actif, dans l'état `RUNNING`, soit
il est inactif, dans l'état `STOPPED`.

En mode `TRACEME`, le _tracee_ passe à l'état `STOPPED` quand il reçoit **n'importe
quel signal**.

Le _tracee_ passe à l'état `RUNNING` quand le père lance la commande `ptrace`
`CONT` (continue).

Le code suivant illustre ce principe :

``` C
int main() {
    pid_t child = fork();
    if(child == 0) {
        ptrace(PTRACE_TRACEME, 0, NULL, NULL);
        child = getpid();
        printf("I am about to get STOPPED\n")
        kill(child, SIGUSR1);
        printf("I am RUNNING again\n");
    }
    else {
        printf("Waiting for the child to stop\n")
        waitpid(child, NULL, 0);
        printf("The tracee is stopped\n")
        ptrace(PTRACE_CONT, child, NULL, NULL);
        // wait for the child to exit
        waitpid(child, NULL, 0);
    }
    return 0;
}
```

Le _tracee_ récupère son pid avec la fonction `getpid` et s'envoie
un signal SIGUSR1 via `kill`. Notons que `kill` est un _syscall_.
A la réception de ce signal, il passe à l'état `STOPPED` car il est en mode
`TRACEME`.

Le _tracer_ fait un premier `waitpid`. `waitpid` permet d'attendre un changement
d'état de son processus fils. Ici, il attend que son fils passe à l'état
`STOPPED`. Notons que `wait` est un _syscall_, ce qui permet au noyau de gérer
sa tambouille interne.

Une fois que `wait` redonne la main, le _tracer_ utilise `PTRACE_CONT` pour que
le _tracee_ repasse à l'état `RUNNING` et continue de s'exécuter.

Le père fait un ultime `wait`. C'est un peu hors propos mais cela permet au
_tracee_ de terminer proprement son exécution, sans rester à l'état zombie.

Nous venons d'illustrer le mécanisme de signaux et de commandes `ptrace` qui
permettent de changer l'état (`RUNNING` / `STOPPED`) du _tracee_.

# Traçons

Lorsque le _tracee_ est à l'état `STOPPED`, `ptrace` fournit au _tracer_ des
commandes qui permettent de l'inspecter et de l'exécuter pas à pas.

- `PEEKUSER` permet d'inspecter les registres du CPU.
Les valeurs de registre ne sont pas lues en live depuis le CPU. En fait, quand le kernel stoppe le tracee il enregistre le contexte du processus, dont les registres, afin que ce dernier puisse reprendre son exécution plus tard, comme si de rien n'était. Les valeurs renvoyées par `ptrace` sont issues de cet enregistrement.

- `PEEKTEXT` permet d'examiner la mémoire.

Le fonctionnement de ces deux commandes est illustré par le code suivant :

```C
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
    }
}

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

int main() {
    long instruction;
    pid_t child = fork();
    if(child == 0) {
        ptrace(PTRACE_TRACEME, 0, NULL, NULL);
        child = getpid();
        kill(child, SIGUSR1);
        fizzbuzz();
    }
    else {
        // wait for the child to stop
        waitchild(child);
        trace(child);
    }
    return 0;
}
```

A l'exécution on a :

    FizzBuzz, 1, 2, Fizz, 4, Buzz, Fizz, 7, 8, Fizz, Buzz, 11, Fizz, 13, 14, FizzBuzz, 16, 17, Fizz, 19, Buzz, Fizz, 22, 23, Fizz, Buzz, 26, Fizz, 28, 29, FizzBuzz, 31, 32, Fizz, 34, Buzz, Fizz, 37, 38, Fizz, Buzz, 41, Fizz, 43, 44, FizzBuzz, 46, 47, Fizz, 49, Buzz, Fizz, 52, 53, Fizz, Buzz, 56, Fizz, 58, 59, FizzBuzz, 61, 62, Fizz, 64, Buzz, Fizz, 67, 68, Fizz, Buzz, 71, Fizz, 73, 74, FizzBuzz, 76, 77, Fizz, 79, Buzz, Fizz, 82, 83, Fizz, Buzz, 86, Fizz, 88, 89, FizzBuzz, 91, 92, Fizz, 94, Buzz, Fizz, 97, 98, Fizz,
    => There are 23037 jumps

Plusieurs exécutions du programme retourne toujours le même nombre, ce qui
est assez rassurant.

Détaillons le programme :

La fonction `main` reprend le même schéma que les exemples précédents :
1. `fork` du process
2. le _tracee_ se met en mode `TRACEME` et passe à l'état `STOPPED` en s'envoyant
n'importe quel signal, puis exécutera `fizzbuzz` quand il passera à l'état `RUNNING`.
3. Le _tracer_ attend que le _tracee_ passe à l'état `STOPPED` puis exécute
`trace`

`fizzbuzz` est une simple fonction qui implémente le célèbre [FizzBuzz](https://en.wikipedia.org/wiki/Fizz_buzz). C'est cette fonction qui
sera auditée par le _tracer_.

`waitchild` encapsule un appel à `waitpid`. Si le _tracee_ passe à l'état
`STOPPED`, elle renvoie 0. Et si le _tracee_ passe à l'état 1, elle renvoie 0.

`trace` est une boucle dont la condition d'arrêt est le _tracee_ qui passe
à l'état `TERMINATED`. Dans cette boucle, le _tracer_ :

1. Utilise la commande `PEEKUSER` afin de récupérer l'adresse de l'instruction
courante stockée dans le registre `RIP`. `PEEKUSER` permet d'inspecter les registres du CPU.
Les valeurs de registre ne sont pas lues en live depuis le CPU. En fait, quand le kernel stoppe le tracee il enregistre le contexte du processus, dont les registres, afin que ce dernier puisse reprendre son exécution plus tard, comme si de rien n'était. Les valeurs renvoyées par `ptrace` sont issues de cet enregistrement.

2. Lit en mémoire, à l'adresse stockée dans `RIP`, l'instruction sur laquelle
le _tracee_ est arrêté, via la commande `PEEKTEXT`.

3. `PEEKTEXT` écrit les octets en mémoire dans un `long` de 8 octets. Notons
que mon système en [little endian](https://en.wikipedia.org/wiki/Endianness),
cela signifie que l'octet à l'adresse pointée par `RIP` est récupéré dans l'octet
de poids de plus faible du `long`. D'où les calculs binaires pour récupérer les
deux premiers octets pointés par `RIP`.

4. On vérifie si l'instruction correspond à une instruction de saut
conditionnel, auquel cas, on incrémente la variable `jmps`.

5. On exécute la commande `SINGLESTEP` qui exécute *une seule* instruction du
_tracee_ et lui envoie un signal `SIGTRAP` pour qu'il passe immédiatement à
l'état `STOPPED`.

6. Après l'exécution de la boucle, on affiche le résultat.

*23000* sauts conditionnels est assez hallucinant, cela en fait 2300 par
itération. `fizzbuzz` est assez simple, mais je pense que `printf` doit
être assez compliqué et faire monter l'addition.

# Tracer n'importe quoi

Jusqu'ici, le _tracee_ était un process bien connu, que nous avions codé nous
même. Ce que nous aimerions, c'est tracer n'importe quel programme.

Le _syscall_ `excecve` permet de remplacer l'image du process appelant par un
autre. A l'issu du _syscall_ `execve`, le process n'a plus rien à voir avec le
code d'origine, il est complètement remplacé par le programme passé à `execve`.
D'ailleurs, il n'y a aucun moyen de récupérer le résultat d'`execve`.

`execve` a 3 paramètres :
- le chemin du programme
- les arguments à passer au programme
- les variables d'environnement à passer au programme

Une subtilité d'`execve` intéressante dans notre cas, est qu'un signal `SIGTRAP`
est automatiquement envoyé après l'exécution d'`execve` si le process est en
mode `TRACEME`. Ce qui siginifie que l'on peut se passer d'envoyer manuellement
un signal dans le _tracee_. Lorsque `waitpid` donne la main au _tracer_, l'image
du _tracee_ a été remplacée par celle du programme passé en paramètre d'`execve`.

``` C
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
```

`waitpid` et `trace` n'ont pas été modifiés, `fizzbuzz` a été supprimé.

`main` a subi quelques altérations :

1. Le _tracee_ s'attend à ce que soient passés le chemin du programme à tracer
dans `argv[1]` et les arguments du programme à tracer dans `argv[2]`, `argv[3]`,
etc.

2. Le _tracee_ ne s'envoie plus de signal lui-même pour passer à l'état `STOPPED`

3. Le _tracee_ appel `execve`.

Le code du _tracer_ n'a absolument pas changé.

A l'exécution, cela donne :

    ./ptrace_ex4 /usr/bin/ls /      
    bin   dev  home  lib64	     media  opt   root	sbin  sys  usr
    boot  etc  lib	 lost+found  mnt    proc  run	srv   tmp  var

    => There are 44633 jumps


# Références

Cet article n'est pas tout à fait original. Ces quelques sources m'ont beaucoup
inspirées, et je vous en conseille la lecture.

- `man 2 ptrace`

- Playing with `ptrace` [http://www.linuxjournal.com/article/6100](Part I), [http://www.linuxjournal.com/article/6210](Part II)

- Nice [https://mikecvet.wordpress.com/2010/08/14/ptrace-tutorial/](write up about ptrace)

- Why [http://lwn.net/Articles/371501/](ptrace is aweful) ?

- How debuggers work
  [part1](http://eli.thegreenplace.net/2011/01/23/how-debuggers-work-part-1),
  [part2](http://eli.thegreenplace.net/2011/01/27/how-debuggers-work-part-2-breakpoints),
  [part3](http://eli.thegreenplace.net/2011/02/07/how-debuggers-work-part-3-debugging-information). En plus l'auteur a mis des références en bas de ses articles, ce que j'apprécie beaucoup ;)

- Explication sur l'implémentation des [hardware breakpoints](https://www.kernel.org/doc/ols/2009/ols2009-pages-149-158.pdf)

- Un autre article sur [le fonctionnement d'un debugger](http://www.alexonlinux.com/how-debugger-works)

- [Implémentation d'un breakpoint](http://mainisusuallyafunction.blogspot.fr/2011/01/implementing-breakpoints-on-x86-linux.html)
