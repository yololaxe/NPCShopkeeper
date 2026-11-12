# Recette solo et multijoueur — boutiques et traversées

Utiliser des noms `RECETTE-`, deux quais sûrs et des personnages de recette. Le backend est celui de préproduction. Vérifier les droits Admin du personnage et les permissions Minecraft. Ne pas mélanger les anciens et nouveaux JAR clients/serveur.

| Cas | Action | Résultat attendu |
|---|---|---|
| S01 | Poser une boutique avec la baguette, choisir un skin et enregistrer | PNJ au point choisi, nom et apparence conservés après rechargement. |
| S02 | Ajouter bougies, briquets et torches par recherche puis par inventaire | Icônes et propriétés correctes ; un ajout répété ne duplique pas le même article. |
| S03 | Faire une boutique de 20 articles puis parcourir les pages | Chaque clic sélectionne l’article affiché, y compris après une recherche. |
| S04 | Prix automatiques globaux, puis ville ; ajouter un prix manuel | Les automatiques suivent le cours choisi ; le manuel reste fixe. |
| S05 | Objet sans référence ou ville sans cours calculé | Article automatique indisponible ; passage au prix manuel possible. |
| S06 | Acheter plusieurs lots avec un mélange de pièces | Quantités, total et monnaie rendue exacts. |
| S07 | Inventaire plein ou pièces insuffisantes | Refus explicite ; aucune perte d’argent, d’objet ou de stock. |
| S08 | Activer le rachat puis le financer ; vendre deux fois le même article | Deux règlements possibles ; budget fini respecté. |
| S09 | Deux joueurs achètent le dernier article simultanément | Une seule attribution ; second joueur informé du stock indisponible. |
| S10 | Consulter le même catalogue plusieurs fois puis annuler | Aucune transaction de marché ajoutée par la consultation ou l’annulation. |
| S11 | Deux admins modifient la même boutique ; vendre pendant qu’un éditeur ancien est ouvert | Version ou stock obsolète refusé ; brouillon conservé sur le site. |
| S12 | Stock limité, recharge 60 s, plafond 32 ; acheter puis attendre | Recharge plafonnée à la consultation suivante ; aucune multiplication des réservations. |
| S13 | Fermer une boutique pendant qu’un joueur la consulte | Aucun nouvel échange sur l’ancien catalogue. |
| S14 | Dupliquer une boutique et enregistrer la copie | Identifiant et stock distincts ; original conservé. |
| S15 | Route avec ville et marchand permanent ; ouvrir puis reprendre une commande après changement des cours | Prix du nouveau contrat territoriaux ; contrat déjà ouvert inchangé. |
| S16 | Terminer un contrat à plusieurs lots et rejouer sa confirmation | Paiement unique, activité unique pour chaque lot livré. |
| S17 | Ouvrir l’atelier sur le site à 320, 390, 768 et 1440 pixels | Pas de débordement horizontal ; tous les réglages restent accessibles. |
| S18 | Interface Minecraft à différentes échelles GUI, clavier et souris | Boutons, liste, saisies, carte et clics restent alignés. |
| V01 | Créer deux ports et les associer à des villes de l’atlas | Positions et comtés corrects ; aucun port homonyme écrasé. |
| V02 | Modifier horaires, compagnie et tarif ; rouvrir le monde | Réglages conservés et vérifiables. |
| V03 | Consulter le capitaine avant/après sa plage ; utiliser l’édition admin hors service | Billetterie disponible seulement à l’ouverture ; édition admin possible au quai. |
| V04 | Royaume → chacun des quatre comtés → ville → retour royaume | Zoom lisible, marqueurs et liste concordants, trajet affiché. |
| V05 | Fermer toutes les destinations puis en rouvrir une | Aucune liaison involontairement réouverte lorsque la liste est vide. |
| V06 | Acheter un billet en pièces ; essayer d’en acheter un deuxième | Monnaie exacte ; une seule réservation active. |
| V07 | Deux passagers pour deux villes desservies, présents au quai au départ | Chacun rejoint son propre quai après la transition. |
| V08 | Manquer le départ, puis reporter au lendemain | Pas de téléportation distante ; report gratuit, même destination. |
| V09 | Déconnexion/reconnexion et redémarrage avant le départ | Billet et prix conservés ; départ manqué reportable. |
| V10 | Dormir pour passer la nuit ou avancer l’heure au-delà du départ | Départ traité ou billet manqué signalé, sans attendre un tick exact. |
| V11 | Annuler avant clôture ; rejouer le remboursement ; inventaire plein | Un seul remboursement effectif ; refus sans perte si les pièces ne tiennent pas. |
| V12 | Réduire l’animation, tenter de bouger et subir une attaque en mer | Transition réduite sans arrivée anticipée ; mouvement bloqué, dégâts neutralisés pendant le passage. |
| V13 | Quitter/rejoindre pendant la transition | Arrivée reprise depuis l’état sauvegardé, sans second paiement. |
| V14 | Bloquer le quai d’arrivée puis le libérer | Arrivée en attente, message utile ; aucune téléportation dans un bloc. |
| V15 | Couper le backend après paiement puis reprendre le reçu | Pas de double débit ; `/mc economie reprendre` puis `/ns harbor ticket` rapproche le billet. |
| V16 | Deux administrateurs enregistrent des modifications concurrentes du port | Seconde version obsolète refusée. |
| V17 | Refaire S06–S16 avec deux joueurs, puis avec trois acheteurs rapides | Stock, budget, reçus et activité restent cohérents ; aucune duplication. |

Ces **35 cas manuels** complètent les tests backend, navigateur et Forge. Une case n’est validée que lorsqu’elle a été réellement exécutée ; conserver une capture et le reçu pour les cas économiques.
