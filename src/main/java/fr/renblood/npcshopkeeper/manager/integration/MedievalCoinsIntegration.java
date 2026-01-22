package fr.renblood.npcshopkeeper.manager.integration;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

public class MedievalCoinsIntegration {
    private static final Logger LOGGER = LogManager.getLogger(MedievalCoinsIntegration.class);
    private static boolean isMedievalCoinsLoaded = false;
    private static Method addJobXpMethod;

    static {
        try {
            Class<?> apiClass = Class.forName("fr.renblood.medievalcoins.api.MedievalCoinsAPI");
            addJobXpMethod = apiClass.getMethod("addJobXp", ServerPlayer.class, String.class, int.class);
            isMedievalCoinsLoaded = true;
            LOGGER.info("MedievalCoins API détectée et chargée avec succès.");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("MedievalCoins API non trouvée (fr.renblood.medievalcoins.api.MedievalCoinsAPI). L'intégration XP sera désactivée.");
        } catch (NoSuchMethodException e) {
            LOGGER.error("Méthode addJobXp non trouvée dans MedievalCoinsAPI.", e);
        }
    }

    public static void addXp(ServerPlayer player, String job, float amount) {
        if (!isMedievalCoinsLoaded) {
            Npcshopkeeper.debugLog(LOGGER, "Tentative d'ajout d'XP annulée : MedievalCoins non chargé.");
            return;
        }
        if (addJobXpMethod == null) {
            Npcshopkeeper.debugLog(LOGGER, "Tentative d'ajout d'XP annulée : Méthode addJobXp introuvable.");
            return;
        }

        try {
            // L'API attend un int, on cast le float (ou on arrondit)
            int intAmount = Math.round(amount);
            if (intAmount > 0) {
                Npcshopkeeper.debugLog(LOGGER, "Appel de MedievalCoinsAPI.addJobXp({}, {}, {})", player.getName().getString(), job, intAmount);
                addJobXpMethod.invoke(null, player, job, intAmount);
                Npcshopkeeper.debugLog(LOGGER, "Appel réussi.");
            } else {
                Npcshopkeeper.debugLog(LOGGER, "Montant d'XP trop faible après arrondi : " + amount + " -> " + intAmount);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'appel à MedievalCoinsAPI.addJobXp", e);
        }
    }
}
