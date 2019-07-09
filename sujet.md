# Objectif général
Dans ce projet, nous allons réaliser un service de stockage distribué. Dans
ce système distribué, plusieurs machines mettent en commun leur capacité de
stockage pour gérer le stockage de fichiers binaires ou textuels. Ces machines
communiquent selon une topologie virtuelle d’arbre binaire : en tant que nœud,
je sais communiquer avec mon sous-nœud gauche et mon sous-nœud droit, mais
je ne sais pas communiquer directement avec tout autre nœuds (voir figure).
Ce service de stockage sera réalisé en RMI. Typiquement, il doit exister un objet
accessible à distance par machine impliquée dans le système. Plus précisément,
un seul objet distant de type Master (interface) sera placé à la racine de l’arbre et
sera le point d’entrée de notre service de stockage. Sur chacun des autres nœuds
de l’arbre sera placé une instance d’objet distant de type Slave (interface). Le
master offrira une interface qui permettra à un programme client :
- de sauvegarder un fichier (type java.io.File) et de le récupérer à partir de
son nom.
- de sauvegarder des données binaires (type byte[])- typiquement extraites
d’un fichier côté client - et de les récupérer à partir du nom du fichier
qu’elle représentent.
- de demander la taille d’un fichier.
Lorsque le master reçoit une demande de sauvegarde de la part d’un client,
l’item à sauvegarder (de type java.io.File ou bien byte[]) doit être divisé et
réparti équitablement sur les slaves disponibles. De façon similaire, lorsque le
master reçoit une demande de récupération, l’item doit être reconstruit à partir
des morceaux répartis dans le système puis renvoyé au client.

![Exemple d’un ensemble de machines pouvant communiquer selon
une topologie virtuelle d’arbre binaire](Capture.PNG)

# Travail demandé
Implémentez le système distribué Tiny DFS avec RMI, à partir du squelette
de code donné ici : https://goo.gl/o4mQf1. Le code pourra facilement être
importé dans un IDE en tant que projet Maven. Additionellement, le service de
stockage proposé devra respecter les spécifications suivantes :
- La sauvegarde d’un item ne doit pas être bloquante pour le client.
- Les slaves ne doivent stocker sur leur disque que les parties de fichiers
dont il sont responsables (strictement un fichier par slave sur disque).
- Les méthodes de récupération retournent null si le fichier spécifié n’existe
pas (cela veut dire qu’il n’a jamais été sauvegardé auparavant).
- Dans le cas où un fichier du même nom est sauvegardé plusieurs fois, la
version la plus récente prend le dessus.
- Le port utilisé pour le protocole RMI sera celui par defaut (1099 ou bien
aussi : `Registry.REGISTRY_PORT`)

Dans tous les cas imaginables, vous pouvez rajouter des classes et des packages
au squelette de code fourni mais vous ne devez pas toucher aux noms des
classes et des packages existants. Votre projet sera soumis à des tests. Les tests
de sauvegarde et de récupération de fichiers et de données binaires vous sont
fournis. Ils se trouvent dans la classe de test `ClientsTest.java`. D’autre part,
vous devez produire, en plus des tests de la classe `ClientsTest.java`, au moins
deux autres tests dans une autre classe :
- Un test qui récupère correctement la taille d’un fichier sauvegardé.
- Un test qui vérifie le bon équilibre de la répartition de la charge de
stockage entre tous les slaves.

# Démarrage du système
Chacun des noeuds (master et slaves) sera démarré individuellement en exécutant 
un programme Java avec  des paramètre donnés (voir code), depuis les
classes MasterMain.java et SlaveMain.java. Par exemple, si j’ai un master et
deux slaves je vais lancer une fois MasterMain.java puis deux fois SlaveMain.java.
Vous devrez compléter ces deux classes pour créer l’objet en question et l’en-
registrer dans un RMI registry. Seul le master sera en charge de créer le RMI
registry, et les slaves s’enregistreront par la suite dans celui-ci. L’enregistrement 
des slaves dans le RMI registry doit se faire sous le nom "slave" auquel
l’identifiant du slave est concaténé (exemples : slave0, slave6, slave35...). Pour
rendre votre système flexible, la méthode main de la classe MasterMain.java
doit prendre trois paramètres :
- Le nom sous lequel notre service de stockage sera disponible dans le RMI
registry.
-  Le chemin vers un répertoire où le master pourra créer des fichiers (pour
y mettre un fichier avant de le renvoyer au client).
- Le nombre de slaves attendu (dans ce projet, nous traitons seulement le
cas où l’arbre binaire est complet : toutes les feuilles de l’arbre sont au
même niveau de profondeur)
La méthode main de la classe SlaveMain.java doit prendre trois paramètres
également :
- Le nom d’hôte du master (pour pouvoir contacter le RMI registry).
- Le chemin vers un répertoire où le slave pourra créer des fichiers (pour y
mettre les morceaux de fichiers à stocker).
- L’identifiant du slave (de 0 à nombre de slaves -1).
Bien sûr, les appels client ne seront possibles que lorsque tous les noeuds ont
été démarrés; nous ne gérons pas le cas où des appels clients parviennent au
master avant le démarrage de tous les noeuds. Vous devez cependant choisir une
stratégie pour la construction de votre arbre : soit petit à petit à chaque fois
qu’un slave est créé, soit lors du premier appel client, etc...

# Evaluation
Un script python, `start-tinydfs.py`, est disponible à la racine du projet
fourni. Il sera utilisé tel quel pour l’évaluation de votre projet. Il permet d’automatiser 
la phase de démarrage expliquée en section 3 (lancement du master et
des slaves) et la phase d’appel client (préparation et lancement des tests). Vous
pouvez vous-même utiliser tout ou partie de ce script dans votre phase de développement, 
mais ce n’est pas obligatoire. Si vous souhaitez l’utiliser, sachez que
ce script prend les mêmes paramètres que le main de MasterMain.java. Il com-
pile le projet via la commande Maven "mvn compile", puis lance la commande
Java qui permet de démarrer le master, et démarre ensuite les slaves dans une
boucle (lignes 42 à 70). En outre, il vérifie que le nombre de slaves spécifié 
correspond bien au nombre de noeuds d’un arbre binaire complet. Après le démarrage
de tous les noeuds, le script procède au démarrage d’appels client (lignes 74 à la
fin). Pour cela, il lance d’abord un programme Java (PropertiesWriter.java)
qui permet d’initialiser des propriétés utilisées dans les tests, puis lance tous les
tests via la commande Maven "mvn test". Vos propres classes de test doivent
donc impérativement se terminer par *Test.java et être placés dans n’importe
quel package dans le répertoire src/test/java. Si vos tests utilisent des 
ressources supplémentaires (typiquement des fichiers), elles devront être placées
dans le répertoire `src/test/resources`.
