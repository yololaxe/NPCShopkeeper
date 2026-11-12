# Ergonomie des marchands et capitaines

- Les boutiques permanentes et les capitaines immobiles regardent progressivement le joueur proche, sans réactiver leurs déplacements. Recherche du joueur toutes les dix ticks, dans un rayon de huit blocs, sans appel API.
- Les boutons − et + ajustent le nombre de lots. Le total suit la saisie. Les bornes tiennent compte des lots complets en stock, de 64 lots maximum et de la limite de 2304 objets par transaction du backend.
- Au premier clic, l’écran apparaît avec « Chargement du comptoir… » pendant la requête. Deux requêtes réseau peuvent être traitées en parallèle par le service, sans blocage du thread serveur.
- Un aperçu privé au joueur, valable trois secondes et limité à 256 entrées, accélère les réouvertures. « Actualiser » force la lecture du backend. Achats, modifications et arrêt du monde invalident le cache ; un ancien résultat en cours ne peut pas le remplir après invalidation.
- Les achats continuent à valider prix et stock sur le backend. Les métadonnées d’édition ne sont plus transmises dans le catalogue joueur. Fermer l’écran pendant l’ouverture empêche le retour tardif de rouvrir le menu.

Recette après installation du JAR NPC côté serveur et client :

1. Approcher un marchand et un capitaine de plusieurs côtés. Ils tournent sans quitter leur place. Vérifier à deux joueurs et avec les skins utilisés.
2. Tester − à un lot, + au maximum, saisie manuelle et stock inférieur à un lot. Vérifier le total puis un achat réel.
3. Ouvrir, fermer et rouvrir rapidement le même marchand ; cliquer « Actualiser ». Pendant une première ouverture, fermer l’écran : il ne doit pas revenir après la réponse.
4. À deux joueurs, acheter la dernière unité pendant qu’un autre consulte le catalogue. Le backend doit refuser le devis devenu obsolète ou le stock insuffisant.

Les tests Forge couvrent les rotations sans déplacement, les bornes des lots, l’expiration, l’isolation, la copie et l’invalidation du cache, en plus des tests existants. Le rendu client et le temps de réponse sur votre serveur restent à vérifier en jeu. Aucun déploiement en production n’est effectué.
