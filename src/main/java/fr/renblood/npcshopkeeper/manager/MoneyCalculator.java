package fr.renblood.npcshopkeeper.manager;

import java.util.List;
import java.util.Map;

public class MoneyCalculator {

    // Méthode pour calculer l'argent total rapporté par un trade
    public static int calculateTotalMoneyFromTrade(List<Map<String, Object>> trades) {
        int totalMoney = 0;

        // Parcourir chaque trade
        for (Map<String, Object> trade : trades) {
            int count = (int) trade.get("quantity");
            int price = (int) trade.get("price");

            // Calculer le total pour ce trade
            totalMoney += count * price;
        }

        return totalMoney;
    }

    // Méthode pour convertir un montant d'argent en pièces
    public static int[] getIntInCoins(int amountInCopper) {
        int[] coins = new int[4]; // Format : [Gold, Silver, Bronze, Copper]

        // Conversion en pièces
        coins[0] = amountInCopper / (64 * 64 * 64); // Gold
        amountInCopper %= (64 * 64 * 64);

        coins[1] = amountInCopper / (64 * 64); // Silver
        amountInCopper %= (64 * 64);

        coins[2] = amountInCopper / 64; // Bronze
        amountInCopper %= 64;

        coins[3] = amountInCopper; // Copper restant

        return coins;
    }
}
