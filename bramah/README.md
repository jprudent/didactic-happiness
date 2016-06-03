# Motivations

Avec le récent scandale des mots de passe volés de LinkedIn (ce ne sont pas les
seuls!), je me suis demandé si mes mots de passe étaient vraiment sûrs. 
J'ai passé en revue trois méthodes pour gérer ses mots de passe.

1. Utiliser le même mot de passe pour tous les sites est sûrement la méthode la
   plus simple. Elle est également la moins sûre car il suffit qu'un seul site
   divulgue votre mot de passe pour compromettre toutes vos autres identités.

2. Utiliser un mot de passe différent pour chaque site en ayant un moyen
   mnémotechnique pour le retrouver. Par exemple, concaténer son nom à celui du site 
   (`jerome_0_gmail`).
   Cette méthode est un peu plus sûre que la précédente mais vos autres
   identités ne résisteront pas bien longtemps à un analyse humaine.
   
3. Utiliser des mots de passe sans aucun lien d'un site à l'autre. C'est la
   méthode la plus sûre mais aussi la plus compliquée en terme de maintenance.
   Pas facile de mémoriser un mot de passe compliqué. Plusieurs outils de
   stockage sont à notre dispositions:

   1. Le carnet que l'on trimbale sur soi et où l'on note tous ses mots de
      passe. Il ne faut surtout pas le perdre.

   2. Un logiciel qui permet de gérer ses mots de passe, que l'on dévérouille
      avec un mot de passe. C'est actuellement la solution que j'utilise mais
      elle me fait flipper. Et si on volait mon mot de passe ? Chaque lettre
      tapée sur un clavier passe en mémoire pour former le mot de passe
      complet. Un virus pourrait être à l'affut ... Le logiciel doit également
      être installé sur toutes les machines qu'on utilise, ce qui n'est pas
      toujours possible.

   3. Il y a carrément des services sur internet pour gérer ses mots de passe.
      Mais bon, j'ai pas confiance, tout comme je n'aurais pas confiance en une
      société tierce qui garderait un double des clés de ma voiture.

Il y a un an je me suis acheté un kit de démarrage Arduino. J'ai joué un peu
avec au début, et ça prend la poussière depuis.

Et si je fabriquais un appareil qui réponde à mon besoin de stocker mes mots de
passe ?

# Le concept

C'est un mélange des trois méthodes citées plus haut :

On déverrouille l'appareil avec un mot de passe, puis on saisit le site pour
lequel on souhaite générer un mot de passe. La combinaison "mot de passe" +
"site" génère toujours le mếme mot de passe.

Les avantages sont les suivants :
- Un seul mot de passe à se rappeler, celui pour dévérouiller l'appareil.
- L'appareil ne stocke aucun mot de passe. Il sait générer le même mot de passe
  à chaque fois. Si quelqu'un vole l'appareil, il doit connaître le mot de passe
  et l'identifiant du site pour générer le même mot de passe.
- C'est une machine, et les machines savent générer des beaux mots de passe
  bien compliqués d'une longueur arbitraire.
- C'est une machine, et les machines savent calculer rapidement pour mettre en
  oeuvre des techniques cryptographiques. Si plusieurs de vos mots de passe
  générés se trimbalent dans la nature, il est impossible d'en déduire le mot
  de passe de dévérouillage de l'appareil. Et donc de générer des mots de passe
  de la même façon.
- L'appareil est déconnecté du réseau. On se met à l'abris de 99% des virus.

Mais il y a des inconvénients :
- Il ne faut pas oublier le mot de passe pour dévérouiller l'appareil, sinon on
  perd toutes ses identités d'un seul coup.
- Il ne faut pas oublier l'identifiant saisi pour chaque site.
- L'appareil génère des mots de passe longs et compliqués. Si un site pose une contrainte
  particulière (et stupide) sur la longueur du mot de passe, ou les caractères
  autorisés (si si, déjà vu) c'est une information qu'il faudra retenir.  
- Si on découvre le code de votre carte bleu et que l'on vous la vole,
  n'importe qui peu retirer de l'argent avec. L'appareil est soumis à la même
  faille, à la différence près qu'il n'est pas possible de faire opposition.

Le mot de passe de dévérouillage peut prendre plusieurs formes combinables (two factor
authentication) :
- Une séquence de touches
- Un badge RFID
- Une lecture d'empreinte digitale

Je ne sais pas si ce concept est original (sûrement pas!) mais je n'ai trouvé
nul part quelque chose de similaire.

# Scénario d'utilisation de l'appareil

J'ai nommé cet appareil `Bramah` en hommage au célèbre sérurrier anglais (que
personne ne connais, sauf wikipedia).

Imaginons que je veuille créer un compte sur Wikipédia. On me demande de saisir
mon mot de passe.

1. Je sors Bramah de ma poche.
2. Je le branche sur un port USB pour l'alimenter (notamment)
3. Je déverrouille Bramah en saisissant le mot de passe.
4. Bramah m'invite à saisir le nom du site : "wikipedia" et je valide.
5. Le mot de passe s'affiche sur l'écran de Bramah.
6. Je suis très courageux et recopie le mot de passe affiché dans la zone de
   saisie de Wikipedia
7. Je suis fainéant et j'appuie sur une touche qui transforme Bramah en clavier
   virtuel et saisi le mot de passe pour moi.

Voici à quoi ressemble Bramah à l'heure actuel.

1. Je sors Bramah de ma poche et met 1 heure pour tout recabler.
2. Je le branche sur un port USB
3. Le mot de passe de déverrouillage est hardcodé dans le programme (si on me
   vole l'appareil, n'importe qui pourra générer des mots de passe à mon insu,
   s'il arrive toutefois à passer l'étape 1)
4. Je saisi "wikipedia" avec le keypad merdique
5. Le mot de passe s'affiche (tronqué à 16 caractères) sur l'écran merdique
6. Je suis une faignasse, je ne recopie pas le mot de passe à la main (en plus
   il est tronqué, je ne vois pas tout)
7. Je positionne mon curseur dans le champ mot de passe de Wikipedia, et j'appuie sur une touche du keypad. Et le mot de passe s'écrit tout seul (ça c'est classe).

J'ai pas mal d'axes d'amélioration en tête. La route est longue mais la voie
est libre.

# Détails sur la génération du mot de passe

Comme je l'ai mentionné dans l'introduction, Bramah est capable de générer de
façon reproductible un mot de passe unique pour la combinaison du mot de passe
de déverrouillage et l'identifiant du site.
Appelons:
- G le mot de passe de déverrouillage
- D l'identifiant du site
- P le mot de passe généré

Si vous avez un minimum de connaissance en cryptographie, vous devez penser
qu'il s'agit d'une fonction de hashage, et vous avez raison.
Je concatène `G+D` et je passe la chaine de caractère obtenu à la fonction de hachage SHA256
pour obtenir P.

    SHA256(G+D) = P

Comme toutes les fonctions de hashage cryptographiques, SHA est un foncion qui
a théoriquement les propriétés suivantes :
- Reproductible : `SHA256(G+D)` produit toujours le même résultat
- Unique : `P` est unique pour toute combinaison G+D, on ne produit jamais le
  même mot de passe pour D1 != D2.
- Irréversible : connaissant P, il est impossible de déduire D+G, G ou D.

Arrêtons-nous 5 minutes sur cette dernière propriété. En fait, vu le scénario
d'utilisation de Bramah, cette dernière propriété porte une faiblesse.
En effet, si Eve (Eve a toujours le mauvais rôle) arrive à voler votre mot de
passe P de wikipédia, elle peut essayer de bourriner pour trouver G. Elle part
du principe que D est "wikipedia" ou "wikipedia.org" ou "wiki". Puis elle
essaye toutes les combinaisons de G jusqu'à ce qu'elle en trouve une qui
satisfasse 

   SHA256(G+"wikipedia") = P

Une fois qu'elle a trouvé G qui est le nom de votre perroquet qui s'appelle "coco",
elle peut alors générer votre mot de passe Twitter, si vous avez choisi
"twitter" comme identifiant de site.

Si un jour j'arrive à miniaturiser Bramah et que je l'utilise vraiment,
j'introduirai un troisième participant, K. K est hardcodé dans l'appareil
et n'est jamais divulgué. La formule de génération sera alors :

   HMAC(K,G+S) = P

Voir l'article wikipédia sur le HMAC.
Je me demande d'ailleurs si G a encore un intérêt cryptographique (il sert de
protection minimale contre le vol de son Bramah).
La cryptographie, ça s'apprend et là je ne suis pas sûr de mon coup.
Mais cela me semble assez robuste si Eve ne fait pas partie des services de
renseignement, qui de toute façon ont d'autres moyens de vous faire parler.

SHA256 génère un mot de passe de 256 bits, soit 32 octets. Chaque octet est un
nombre variant entre 0 et 255. Pour que le mot de passe soit affichable, j'ai
encodé chacun de ces nombres sur une plage affichable de la table ASCII (du
caractère '~' au caractère '!').

Le code qui génère le mot de passe est dans le fichier `PasswordGenerator.cpp`.
Je ne suis pas bon en C++, mais si vous l'êtes, n'hésitez pas à commenter mon code.

# Le keypad

La solution keypad est une solution de repli. J'ai utilisé le keypad fourni
avec mon kit de démarrage Arduino. En terme de design, je pense que c'est le
pire truc qui puisse exister. Il est gigantesque. Il a sans doute été conçu pour 
taper aussi bien avec son index que son orteil.

Le branchement est trivial. 8 fils branchés sur 8 pins de l'Arduino. 
4 pour les colonnes, 4 pour les lignes. On doit sûrement pouvoir économiser
quelques pins en multiplexant tout ça, mais je ne me suis pas pris la tête.

Je suis parti d'un exemple fournit avec la librairie Keypad toute faite
disponible avec l'IDE Arduino. J'ai un peu modifié l'exemple pour arriver à
faire une API simple. L'API consiste en 2 callbacks:
- l'une permet d'être notifié quand une touche est pressée.
- l'autre permet d'être notifié quand une touche est pressée plus de 500ms.

La touche '#' pressée plus de 500 ms permet de passer du clavier numérique au 
clavier alphabétique.

Le code se trouve dans le fichier `MultitapKeypad.cpp`

# L'écran

Là encore j'ai utilisé l'écran LCD 16 colonnes 2 lignes fournit avec mon kit.
Le montage est celui proposé sur [la page du site
arduino](https://www.arduino.cc/en/Tutorial/HelloWorld?from=Tutorial.LiquidCrystal).

J'ai créer une API de haut niveau qui s'appuie sur la librairie
`LiquidCrystal`. L'API propose quatre méthode :
- `void append(char)` pour ajouter un caractère
- `void append(char *) pour ajouter une chaine
- `void replace(char)` pour remplacer le dernier carctère
- `void erase()` pour effacer le dernier caractère
- `char* getLine1()` pour récupérer la ligne 
- `void reset()` pour tout effacer et reparir à zéro

Tout le code se trouve dans le fichier `Display.cpp`.

# Le clavier virtuel

La carte de développement possède en fait deux microcontroleurs Atmel. 

- Le plus gros est un ATmega328P. C'est celui là qui fait tourner notre
  programme. Mais il n'est pas connecté directement à l'USB.

- Le plus petit est un ATmega16u2. Son rôle est de recevoir le programme à
  envoyer par le port USB et de l'écrire dans la mémoire flash de l'ATmega328P.

Il existe un firmware alternatif pour l'ATmega16u2 qui implémente la
spécification HID.
La spécification HID définit un protocol de communication USB générique qui
permet de développer des périphériques tels qu'un clavier ou une souris. Le
système d'exploitation a un driver HID générique capable de comprendre
n'importe quel périphérique qui implémente HID. C'est la raison pour laquelle
quand on branche un clavier ou une souris USB, elle est reconnue à chaud sans
nécessiter de driver particulier.

Quand on flashe l'ATmega16u2 avec le firmware HID et qu'on branche l'arduino
sur le port USB de l'ordinateur, il est reconnu comme périphérique HID.
Le firmware HID de l'ATmega16u2 reçoit les commandes en série de l'ATmega328P
qui les transmet par USB à l'ordinateur.  

Malheureusement, une fois flashé, l'ATmega16u2 n'est plus capable de programmer
l'ATmega328P comme à l'usuel. Il faut donc restaurer le firmware d'origine du
ATmega16u2 pour écrire nos programmes sur l'ATmega328P.

Le cycle de développement est donc :
1. Ecrire un programme qui communique des commandes HID via le port série à
   l'ATmega16u2.
2. Flasher l'ATmega16u2 avec le firmware HID.
3. Rebooter la carte Arduino
4. Tester que notre programme simule correctement un clavier.
6. Flasher l'ATmega16u2 avec le firmware d'origine
7. Rebooter la carte Arduino
8. Retour en 1)

C'est un peu pénible mais on y arrive!

La procédure pour flasher l'ATmega16u2 est similaire au
flashage de l'[ATmega8u2](https://www.arduino.cc/en/Hacking/DFUProgramming8U2)
qu'il faut adapter à l'ATmega16u2 de la carte Arduino R3.

Il faut passer en mode [DFU](://en.wikipedia.org/wiki/USB#DFU) (Device Firmware Update).Ce mode USB est spécialement conçu pour la mise à jour de firmware. Pour cela il suffit de
relier la pin "reset" de l'ATmega16u2 à la masse. 

Ensuite il suffit d'envoyer le nouveau firmware via USB grâce à un utilitaire
qui s'appelle `dfu-programmer`. Pour mon Archlinux, il n'était pas présent dans
les repos officiels, mais il est dispo dans un
[AUR](https://aur.archlinux.org/packages/dfu-programmer/). Pour le reste des
mortels (qui utilise un système inférieur au mien), la [page sur le site 
d'Arduino](https://www.arduino.cc/en/Hacking/DFUProgramming8U2) vous dira comment l'installer.

La séquence d'instructions suivantes efface le firmware actuel, écrit le
nouveau et redémarre le microcontrôleur :

    sudo dfu-programmer atmega16u2 erase
    sudo dfu-programmer atmega16u2 flash --debug 1 Arduino-keyboard-0.3.hex
    sudo dfu-programmer atmega16u2 reset

La seconde commande flash le microcontrolleur avec un firmware contenu dans le
fichier `Arduino-keyboard-0.3.hex`.
Quand on cherche des informations pour simuler un clavier avec un Arduino Uno,
on finit toujours par arriver sur la page de [Mitch
Tech](http://mitchtech.net/arduino-usb-hid-keyboard/). C'est là que j'ai chopé
le firmware sans trop me poser de questions.

Bon maintenant qu'on sait faire passer l'Arduino pour un clavier, parlons un
peu de code.

J'ai créé une API de haut niveau qui n'a qu'une seule méthode, `void
type_on_keyboard(char*)` qui permet au clavier virtuel de saisir une phrase.

Concernant les claviers, la norme HID définit une table de correspondance entre
un ID et un caractère. On peut trouver cette table dans la [documentation
officielle](http://www.usb.org/developers/hidpage/Hut1_12v2.pdf) à la rubrique
"Keyboard/Keypad Page (0x07)". Cette table nous apprend par exemple que la
lettre 'a' correspond à l'id 4.

Ma fonction `type_on_keyboard` parcourt la chaine en entrée. Pour chaque
caractère ASCII, elle détermine l'ID de la lettre correspondant et l'envoie sur
le port série via `Serial.write`.

Le code se trouve dans le fichier `HidKeyboard.cpp`. 

# Recoller les morceaux

On a passé en revu comment récupérer les touches saisies au keypad, comment
controler l'écran et comment simuler un clavier. Il ne reste plus qu'à mélanger
tout ça pour créer Bramah.


h1. How to

In order to be able to act as a HID keyboard, the arduino uno little CPU that acts as USB bridge has to be flashed with a keyboard HID firmware. But, with a keyboard HID firmware, we can't send new sketch, so we have to flash it again to the original firmware.

There is two scrips :
flash-keyboard.sh
flash-original.sh

Before launching any of them, we must put the little CPU in flashing mode,
making briefly a bridge between RST and GND. Hard reboot is necessary after
each flash.

h1. Bugs

- The '\' is printed as the Yen symbol.

- Only 16 characters are printed on scree



h1. Diary

#. Day 8
Bought components on the internet. Notably : 
- Arduino nano : I expect it to work like UNO and be able to flash it to act as
  a keyboard
- Arduino micro : Apparently less powerful but ship comes with USB keyboard for
  free.
No Code :(

#. Day 6-7
The weekend. Display is OK. I mean I can type on keypad and characters are
output on LCD. I struggle a lot with C++ because I program without having an in
depth knowledge of the language. The wiring of the LCD is painful.
Next step is to implement backspace and enter.

#. Day 5
Got a multitap keypad. But I don't like it. Usage is horrible and so is the
underlying code. A T9 style input would be so much better. And the Keypad 
library is unsuitable for my needs (is it?). And there is no backspace,
*no bullshit*.

#. Day 4
Spent some time finding a nice way to have input. I opt for a keypad with
multitap, but I'm too lazy to try anything this evening.
The Keypad library that comes with arduino can support this. There is an
example called DynamicKeypad.ino.

#. Day 3
I finally managed to generate a password and type it on keyboard.
I have to check that my mapping to hid is correct.

#. Day 2
Tried to use the Keyboard library but it doesn't work with Uno
Found 2 workarounds :
- A soft usb :
  http://blog.petrockblock.com/2012/05/19/usb-keyboard-with-arduino-and-v-usb-library-an-example/
But I need some electronic components
- Flash the ATMEGA that control the USB. Seems tedious.
  http://mitchtech.net/arduino-usb-hid-keyboard/

Finally, opted for flashing the USB controller.
Now, I have to use http://www.usb.org/developers/hidpage/Hut1_12v2.pdf in order
to output the correct letter. 

#. Day 1

Had a first POC : can generate a SHA256 and Base64 the output
