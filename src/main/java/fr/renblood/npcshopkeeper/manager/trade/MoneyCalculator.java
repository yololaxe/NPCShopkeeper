package fr.renblood.npcshopkeeper.manager.trade;

import java.util.List;
import java.util.Map;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MoneyCalculator {

    private static final Logger LOGGER = LogManager.getLogger(MoneyCalculator.class);

    // Méthode pour calculer l'argent total rapporté par un trade
    public static long calculateTotalMoneyFromTrade(List<Map<String, Object>> trades) {
        long total=0;
        for(Map<String,Object> trade:trades){
            if(!(trade.get("quantity") instanceof Integer count)||!(trade.get("price") instanceof Number number)||count<1)
                throw new IllegalArgumentException("Quantité et prix entiers positifs requis");
            long price=new java.math.BigDecimal(number.toString()).longValueExact();
            if(price<1)throw new IllegalArgumentException("Prix positif requis");
            total=Math.addExact(total,Math.multiplyExact((long)count,price));
        }return total;
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
