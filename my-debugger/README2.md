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

TODO

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
commandes qui permettent de l'inspecter.

`PEEKUSER` permet d'inspecter les registres du CPU.
Les valeurs de registre ne sont pas lues en live depuis le CPU. En fait, quand le kernel stoppe le tracee il enregistre le contexte du processus, dont les registres, afin que ce dernier puisse reprendre son exécution plus tard, comme si de rien n'était. Les valeurs renvoyées par `ptrace` sont issues de cet enregistrement.

 
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
