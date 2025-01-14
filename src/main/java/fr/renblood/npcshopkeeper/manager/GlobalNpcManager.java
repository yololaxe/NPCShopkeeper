package fr.renblood.npcshopkeeper.manager;

import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static fr.renblood.npcshopkeeper.manager.ActiveNpcManager.addActiveNpc;

public class GlobalNpcManager {
    private static final Logger LOGGER = LogManager.getLogger(GlobalNpcManager.class);

    private static final Set<String> activeNpcs = new HashSet<>();
    private static final List<String> inactiveNpcs = new ArrayList<>();
    private static final Map<String, Map<String, Object>> npcDataMap = new HashMap<>();

    public static int getDataSize() {
        return npcDataMap.size();
    }

    // Charger les données du fichier JSON au démarrage
    public static void loadNpcData() {
        npcDataMap.clear();
        Map<String, Map<String, Object>> loadedData = JsonTradeFileManager.getPnjData();
        if (loadedData.isEmpty()) {
            LOGGER.error("Aucune donnée PNJ chargée depuis le JSON !");
            return;
        }

        npcDataMap.putAll(loadedData);
        inactiveNpcs.clear();
        inactiveNpcs.addAll(npcDataMap.keySet());
        activeNpcs.clear();

        LOGGER.info("Données PNJ chargées avec succès. Total PNJs : " + npcDataMap.size());

        // Synchroniser avec ActiveNpcManager
    }

    public static void activateNpc(TradeNpc npc) {
        if (inactiveNpcs.remove(npc.getNpcName())) {
            activeNpcs.add(npc.getNpcName());
            LOGGER.info("PNJ activé : " + npc.getNpcName());
            addActiveNpc(npc);
        }
    }

    public static void deactivateNpc(String npcName) {
        if (activeNpcs.remove(npcName)) {
            inactiveNpcs.add(npcName);
            LOGGER.info("PNJ désactivé : " + npcName);
            removeNpcFromActiveManager(npcName);
        }
    }

    public static String getRandomInactiveNpc() {
        if (inactiveNpcs.isEmpty()) {
            LOGGER.warn("Aucun PNJ inactif disponible.");
            return null;
        }
        // Sélection aléatoire d'un PNJ dans la liste des inactifs
        Random random = new Random();
        String randomNpcName = inactiveNpcs.get(random.nextInt(inactiveNpcs.size()));

        LOGGER.info("PNJ inactif aléatoire sélectionné : " + randomNpcName);
        return randomNpcName;
    }

    public static Map<String, Object> getNpcData(String npcName) {
        return npcDataMap.getOrDefault(npcName, null);
    }

    public static Set<String> getActiveNpcs() {
        return Collections.unmodifiableSet(activeNpcs);
    }

    public static List<String> getInactiveNpcs() {
        return Collections.unmodifiableList(inactiveNpcs);
    }


    private static void removeNpcFromActiveManager(String npcName) {
        Map<String, Object> npcData = getNpcData(npcName);
        if (npcData == null) {
            LOGGER.error("Impossible de retirer le PNJ du ActiveNpcManager, données introuvables pour : " + npcName);
            return;
        }

        UUID uuid = UUID.fromString((String) npcData.get("id"));
        ActiveNpcManager.removeActiveNpc(uuid);
    }
}
