# Prix PNJ et horloge — recette complémentaire

Installer les trois JAR du correctif, puis redémarrer le client Minecraft.

1. Dans `/mc admin config info`, vérifier la durée du cycle (x3 = 60 minutes), l’heure, le sommeil et les délais API. Aucune clé ne doit être affichée.
2. Dans un monde de recette, régler `day-length 2`, `sleep-percentage 65` et `sleep-duration 7` sous `/mc admin config time`. Quitter proprement le monde et revenir : vérifier 40 minutes, 65 % et 7 secondes. Remettre ensuite les réglages souhaités.
3. À Creed, actualiser le marchand : le pain doit utiliser son cours, les bougies sans référence doivent indiquer « Prix à définir » et ne pas être achetables à zéro.
4. Administrateur : ouvrir « Définir le prix » ou Maj + clic droit, choisir la bougie, saisir un prix manuel en fer par objet et enregistrer. Avec des pièces dans l’inventaire, acheter un lot puis vérifier objet, monnaie et reçu.
5. Depuis un compte joueur non administrateur, vérifier qu’aucune action de modification du prix n’est proposée. Tester deux achats simultanés de la dernière unité ; un seul doit réussir.
6. Dans l’atelier du site, passer tous les prix en automatique : un article sans cours doit conserver son prix manuel. Changer de ville : une ancienne cotation automatique absente dans la nouvelle ville doit devenir un brouillon manuel indisponible.
7. Pour automatiser une bougie, créer sa référence, relancer le calcul des marchés, actualiser et activer le prix automatique. Vérifier qu’un recalcul seul ne remplace pas un prix déjà manuel.
8. Réserver une traversée au crépuscule : avant la dernière minute, aucun compte à rebours permanent. Pendant la dernière minute réelle, petit rappel en haut à droite, masqué dans les menus. Tester avec deux facteurs de durée et vérifier le départ au quai, le report gratuit après absence et la reprise du billet après reconnexion.

Les essais automatiques couvrent les règles de prix, les échanges, l’horloge et la sauvegarde. Les actions Minecraft avec inventaires réels, deux joueurs et packs graphiques restent à effectuer dans cette recette.
