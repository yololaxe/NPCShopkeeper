package fr.renblood.npcshopkeeper.manager.npc;

import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActiveNpcManager {
    private static final Logger LOGGER = LogManager.getLogger(ActiveNpcManager.class);
    private static final Map<UUID, TradeNpc> activeNpcs = new HashMap<>();

    // Ajouter un PNJ actif
    public static void addActiveNpc(TradeNpc npc) {
        activeNpcs.put(UUID.fromString(npc.getNpcId()), npc);
        LOGGER.info("PNJ ajouté aux actifs : " + npc.getNpcName() + " (ID: " + npc.getNpcId() + ")");
    }

    // Supprimer un PNJ actif
    public static void removeActiveNpc(UUID uuid) {
        TradeNpc removedNpc = activeNpcs.remove(uuid);
        if (removedNpc != null) {
            LOGGER.info("PNJ retiré des actifs : " + removedNpc.getNpcName() + " (ID: " + uuid + ")");
        } else {
            LOGGER.warn("Aucun PNJ trouvé avec l'UUID : " + uuid);
        }
    }

    // Récupérer un PNJ actif
    public static TradeNpc getActiveNpc(UUID uuid) {
        return activeNpcs.get(uuid);
    }

    // Vérifier si un PNJ est actif
    public static boolean isNpcActive(UUID uuid) {
        return activeNpcs.containsKey(uuid);
    }

    // Imprimer tous les PNJs actifs
    public static void printActiveNpcs() {
        if (activeNpcs.isEmpty()) {
            LOGGER.info("Aucun PNJ actif actuellement.");
        } else {
            LOGGER.info("Liste des PNJs actifs :");
            for (TradeNpc npc : activeNpcs.values()) {
                LOGGER.info("PNJ : " + npc.getNpcName() + " (ID: " + npc.getNpcId() + ")");
            }
        }
    }

}
