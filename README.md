NpcShopkeeper for Renblood Server

Actual command : 
# 📦 Système de Commerce et de Routes Commerciales

Ce module permet de configurer des échanges personnalisés, de créer des itinéraires pour des marchands ambulants et de gérer une économie dynamique via des fourchettes de prix.

---

## 🛠️ Gestion des Trades (Échanges)

Permet de définir les objets vendus par les PNJs via une interface graphique.

| Commande | Usage | Description |
|:---|:---|:---|
| `/createreference <nom> <catégorie>` | `/createreference epee_fer forgeron` | Ouvre un GUI pour placer l'item à vendre et l'item requis. |
| `/trade <nom> <npc_id> <npc_name>` | `/trade epee_fer 12345 Forgeron` | **Debug** : Force l'ouverture de l'interface sans PNJ pour test. |

---

## 🛣️ Gestion des Routes (PNJs Voyageurs)

Configurez les zones de passage et d'apparition des marchands itinérants.

### Configuration des points
* **Créer une route** : `/create_commercial_road <nom_de_la_route> <catégorie>`
    * *Exemple :* `/create_commercial_road route_du_nord fermier`
* **Ajouter un point** : `/add_point_to_road <nom_de_la_route>`
    * *Note :* Ajoute votre position actuelle comme point de passage (waypoint). Les PNJs spawneront aléatoirement sur ces points.

### Visualisation
* **Afficher la route** : `/show_road <nom_de_la_route>`
    * Affiche des particules en jeu pour visualiser les points de passage et vérifier le tracé.

---

## 💰 Économie et Prix

Gérez la fluctuation des prix pour rendre le commerce plus vivant.

* **Définir une fourchette de prix** : 
    * ` /set_price_reference <item> <min> <max>`
    * *Exemple :* `/set_price_reference minecraft:iron_ingot 5 10`
    * *Fonctionnement :* Le prix final lors d'un échange sera choisi aléatoirement entre la valeur minimale et maximale définie.

---

> [!IMPORTANT]
> Lors de l'utilisation du `/createreference`, assurez-vous d'avoir les items dans votre inventaire pour les placer dans l'interface de configuration.
