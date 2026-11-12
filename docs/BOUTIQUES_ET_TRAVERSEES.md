# Boutiques PNJ et traversées

## Administrateur : une boutique permanente

1. Dans l’onglet créatif NPC Shopkeeper, prendre **Baguette des boutiques**. Clic droit sur le sol à l’emplacement choisi. `/ns shop create` utilise la position du joueur.
2. Donner un nom, choisir une apparence existante avec **Apparence**, ou saisir un chemin de texture compatible. Choisir une ville ou **Aucune ville · cours commun**.
3. **Ajouter des articles** : recherche dans les objets du jeu ou ajout de l’inventaire en une action. Les propriétés des objets sont conservées. Une boutique accepte 54 articles, avec pagination.
4. Sélectionner un article : vente et rachat indépendants, prix automatiques ou prix manuels en fer par objet, quantité par lot. Le stock `-1` est illimité ; un entier positif ou zéro définit un stock fini. La recharge automatique, si choisie, a un intervalle d’au moins 60 secondes réelles et un plafond. `0` l’arrête. Elle est appliquée à la consultation suivante, sans faire tourner un timer Minecraft par article.
5. **Enregistrer la boutique**. Maj + clic droit sur le marchand ouvre l’atelier. Le bouton de duplication prépare une nouvelle boutique à proximité ; il faut encore l’enregistrer.

Le joueur consulte le catalogue, sélectionne un article et un nombre de lots, puis confirme. Onglets d’achat et de rachat distincts ; règlement en pièces et monnaie rendue. Un inventaire incapable de recevoir l’achat ou les pièces annule la remise. Les achats simultanés réservent réellement le stock dans l’économie.

Le rachat est financé par la caisse `npcshop:<UUID de la boutique>`. Utiliser Tab dans `/mc economie budget` pour retrouver l’établissement. L’illimité est un choix explicite de l’intendance ; le stock illimité d’articles n’active pas un budget de rachat illimité.

Le site propose le même catalogue à `/admin/npc-shops`, accessible depuis le widget Marchés. Les modifications de catalogue ne sont pas écrasées silencieusement si une autre personne ou le stock a évolué. Les droits Admin du personnage backend sont nécessaires, en plus des droits administrateur Minecraft.

## Villes et contrats

`/ns road economy` permet d’associer une ville à une route. Ses marchands héritent de ce rattachement. Un marchand de contrats permanent propose son catalogue par pages de six ; l’administrateur peut changer sa ville depuis ce catalogue. Un marchand de passage conserve une seule commande.

Une nouvelle commande prend les cours calculés du marché de la ville ; sans ville, elle utilise le cours commun. Les contrats déjà ouverts conservent leurs quantités, leurs prix et leur ville dans leur historique sauvegardé. Une référence ou un cours local absent suspend l’offre, sans substitution silencieuse.

Les prix manuels d’une boutique restent fixes. Les prix automatiques suivent le moteur existant ; le rachat automatique est à 80 % de la vente. Seuls les échanges confirmés sont ajoutés à l’activité du marché, une fois par reçu. Les consultations, annulations et réapprovisionnements administratifs ne sont pas des transactions de joueurs.

Le mode « économie dynamique » conserve son réglage existant dans l’administration des marchés. Lorsqu’il est activé, la première consultation PNJ après quinze minutes planifie un recalcul commun en arrière-plan. Un verrou Mongo partagé évite un recalcul par joueur ou processus web ; les cours précédents sont servis pendant le calcul. Les variations et seuils du moteur des marchés restent applicables. Aucun réglage global existant n’est activé ou réinitialisé silencieusement par la mise à jour.

## Administrateur : un capitaine

1. Se placer sur un quai sûr, avec deux blocs d’air et un sol, puis `/ns harbor create "Nom du port"`.
2. Maj + clic droit sur le capitaine, ou `/ns harbor edit "Nom du port"` au quai, y compris hors des horaires de présence.
3. Saisir le nom de la ville de l’atlas pour reprendre automatiquement son comté et sa position sur la carte. Les quatre comtés viennent du site : Mofrage, Palam, Eldia et Cornwall.
4. Choisir la compagnie et les heures **HH:MM**. Par défaut : ouverture 08:00, clôture 17:30, départ 18:00. Il s’agit de l’heure du monde Minecraft, pas de l’heure réelle.
5. Choisir une destination sur la carte ou dans la liste : ouvrir/fermer la liaison et fixer un tarif en fer, ou `0` pour un calcul par distance entre les quais. Enregistrer. Un port non placé sur l’atlas reste accessible dans la liste pour pouvoir le configurer.

Les anciennes données de ports reçoivent ces horaires par défaut, sans déplacement des quais. Un nom de port existant ne peut plus être remplacé par une nouvelle création. Le capitaine conserve son point et devient invisible hors de sa plage de présence ; l’édition administrative reste disponible par commande.

## Joueur : réserver et embarquer

1. Clic droit sur le capitaine pendant l’ouverture.
2. Vue du royaume ; choisir un comté, puis une ville desservie. La liste complète les marqueurs. La liaison et le tarif sont affichés avant de payer.
3. Confirmer le paiement en pièces : le billet est sauvegardé, le tarif est figé et le compte à rebours est affiché. Une seule réservation active par joueur.
4. Revenir à moins de 10 blocs du quai pour le départ. Un rappel est envoyé avant l’embarquement.
5. Au crépuscule, transition maritime de dix secondes, avec option de réduction de l’animation. Le serveur confirme l’arrivée au quai de destination ; le mouvement est bloqué et les dégâts sont neutralisés pendant la traversée.

Un départ manqué ou une déconnexion conserve un billet **à reporter gratuitement** auprès du capitaine du port d’origine. L’annulation avant la clôture, ou après un départ manqué, rembourse le billet en pièces via le reçu original. Un second remboursement est refusé. Le billet ne change pas de destination au report.

`/ns harbor ticket` consulte le billet et rapproche un règlement confirmé après `/mc economie reprendre` si une réponse a été perdue. Un saut de nuit déclenche les départs dépassés lors du prochain contrôle ; une heure exacte ne doit pas être traversée à un tick précis. Une arrivée dangereuse reste en attente et signale le quai à vérifier, sans téléportation dans un bloc.

**Simuler mon départ** est réservé aux administrateurs et utilise leur billet réel de recette. Créer deux ports de recette ; aucun port, personnage ou solde existant n’est créé ou modifié automatiquement.

## Compatibilité et recette

Le protocole NPC Shopkeeper passe à **3** : installer le même nouveau JAR sur les clients et le serveur, avec la nouvelle passerelle Medieval Coins. Medieval Structures reste compatible et conserve ses auberges et clients.

Voir [RECETTE_BOUTIQUES_TRAVERSEES.md](RECETTE_BOUTIQUES_TRAVERSEES.md). Les tests automatiques ne remplacent pas les essais visuels avec deux joueurs, la carte et les pièces de vos packs.


## Prix absents et horloge (correctif du 12 novembre)

Un article sans référence ou sans cours calculé pour la ville passe en **prix manuel**.
L’atelier propose de saisir le montant en fer par objet. Un prix nul est un brouillon,
jamais une gratuité : le côté non tarifé ne peut pas être échangé. Le prix automatique
pour tous ne s’applique qu’aux articles possédant un cours. Un administrateur peut aussi
ouvrir « Définir le prix » depuis un article indisponible du catalogue joueur.

Pour automatiser un objet : créer/activer sa référence dans l’administration du site,
relancer le calcul dans Marchés et économie, actualiser le marchand puis activer le prix
automatique. Le recalcul n’invente pas de référence manquante et ne remplace pas un prix manuel.
Il n’est pas nécessaire après une simple saisie de prix manuel.

Les départs restent au crépuscule. Le HUD affiche uniquement la dernière minute,
en petit dans l’angle supérieur droit, en dehors des menus. Un rappel invite à rejoindre
le quai. Le calcul respecte le facteur de durée de Medieval Coins ; la synchronisation
réseau s’effectue toutes les dix secondes, sans requête HTTP. `/ns harbor ticket` permet
de consulter le billet pendant toute l’attente. Un cycle ralenti et le sommeil peuvent
modifier le temps d’attente ; le serveur reste l’autorité pour le départ.

`/mc admin config info` (ou `/mc admin config`) affiche la durée du cycle jour+nuit
à 20 TPS, l’heure, les conditions de sommeil, l’adresse API et les délais/cache/synchronisation.
Les clés ne sont jamais affichées. Les réglages d’horloge sont maintenant conservés **par monde** ;
les anciennes commandes les perdaient au redémarrage. Sans réglage sauvegardé, le facteur est
toujours x3 (60 minutes par cycle), avec 80 % des joueurs au lit pendant cinq secondes.
`/mc admin config time day-length 1` donne un cycle de 20 minutes ; `3` donne 60 minutes.
