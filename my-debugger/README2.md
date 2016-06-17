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

Le processeur plusieurs niveaux d'exécution :

- Typiquement le noyau Linux tourne dans un mode dit _privilégié_. Dans ce mode
il peut accèder à toute la mémoire, lire et écrire sur disque, ...

- Les autres process, comme votre navigateur, tournent dans un mode
_non privilégié_. Ils n'ont accès qu'à une certaine partie de la mémoire et ne
peuvent pas écrire directement sur disque.

Tant qu'un process se contente de faire des calcul et de lire et écrire en
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

L'exemple suivant illustre un appel au _syscall_ `write` :

``` asm
mov    $0x1,%rax
mov    $0x1,%rdi
mov    $0x4000fe,%rsi
mov    $0xd,%rdx
syscall
```

Traduit en français, cela donne : "Appel du syscall `sys_write` (RAX=1) pour écrire dans le file descriptor 1 (RDI=1), alias la sortie standard, la chaîne de caractère à l'adresse 0x4000fe (RSI=0x4000fe) de longueur 13 (RDX=0xd). Notez l'instruction `syscall` qui est une vraie instruction assembleur x86_64.

# Salut fiston, c'est papa !

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
