package fr.renblood.npcshopkeeper.manager.trade;

import java.util.List;
import java.util.Map;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MoneyCalculator {

    private static final Logger LOGGER = LogManager.getLogger(MoneyCalculator.class);

    // Méthode pour calculer l'argent total rapporté par un trade
    public static int calculateTotalMoneyFromTrade(List<Map<String, Object>> trades) {
        int totalMoney = 0;
        Npcshopkeeper.debugLog(LOGGER, "Début du calcul de l'argent total pour le trade.");

        // Parcourir chaque trade
        for (Map<String, Object> trade : trades) {
            try {
                // Vérification des clés dans chaque Map
                if (!trade.containsKey("quantity") || !trade.containsKey("price")) {
                    LOGGER.error("Clés manquantes dans le trade : " + trade);
                    continue; // Passer à l'élément suivant si les clés ne sont pas présentes
                }

                // Assurez-vous que les types sont corrects
                Object countObj = trade.get("quantity"); // Utilisation de "count" au lieu de "quantity"
                Object priceObj = trade.get("price");

                // Vérifier que les valeurs sont bien des entiers
                if (!(countObj instanceof Integer) || !(priceObj instanceof Integer)) {
                    LOGGER.error("Les types ne sont pas des entiers. Quantité : " + countObj + ", Prix : " + priceObj);
                    continue; // Passer à l'élément suivant si les types sont incorrects
                }

                int count = (int) countObj;
                int price = (int) priceObj;

                // Calculer le total pour ce trade
                int tradeValue = count * price;
                totalMoney += tradeValue;

                Npcshopkeeper.debugLog(LOGGER, "Trade - Item: " + trade.get("item") + ", Quantity: " + count + ", Price: " + price + ", Total pour cet item: " + tradeValue);

            } catch (Exception e) {
                LOGGER.error("Erreur lors du calcul de l'argent pour un trade : " + trade, e);
            }
        }

        Npcshopkeeper.debugLog(LOGGER, "Total d'argent calculé pour le trade : " + totalMoney + " en cuivre.");
        return totalMoney;
    }



    // Méthode pour convertir un montant d'argent en pièces
    public static int[] getIntInCoins(int amountInCopper) {
        Npcshopkeeper.debugLog(LOGGER, "Conversion de " + amountInCopper + " cuivre(s) en pièces.");
        int[] coins = new int[4]; // Format : [Gold, Silver, Bronze, Copper]

        // Conversion en pièces
        coins[0] = amountInCopper / (64 * 64 * 64); // Gold
        Npcshopkeeper.debugLog(LOGGER, "Nombre de pièces d'or calculé : " + coins[0]);
        amountInCopper %= (64 * 64 * 64);

        coins[1] = amountInCopper / (64 * 64); // Silver
        Npcshopkeeper.debugLog(LOGGER, "Nombre de pièces d'argent calculé : " + coins[1]);
        amountInCopper %= (64 * 64);

        coins[2] = amountInCopper / 64; // Bronze
        Npcshopkeeper.debugLog(LOGGER, "Nombre de pièces de bronze calculé : " + coins[2]);
        amountInCopper %= 64;

        coins[3] = amountInCopper; // Copper restant
        Npcshopkeeper.debugLog(LOGGER, "Nombre de pièces de cuivre restantes : " + coins[3]);

        Npcshopkeeper.debugLog(LOGGER, "Conversion terminée. Résultat : Or = " + coins[0] + ", Argent = " + coins[1] + ", Bronze = " + coins[2] + ", Cuivre = " + coins[3]);
        return coins;
    }
}
