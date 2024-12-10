package fr.renblood.npcshopkeeper.manager;



import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

import java.util.*;

import static com.mojang.text2speech.Narrator.LOGGER;

public class GlobalNpcManager {
    private static final Logger LOGGER = LogManager.getLogger(GlobalNpcManager.class);

    private static final Set<String> activeNpcs = new HashSet<>();
    private static final List<String> inactiveNpcs = new ArrayList<>();
    private static final Map<String, Map<String, Object>> npcDataMap = new HashMap<>();

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
    }

    public static Optional<String> getRandomInactiveNpc() {
        if (inactiveNpcs.isEmpty()) {
            LOGGER.warn("Aucun PNJ inactif disponible.");
            return Optional.empty();
        }
        Random random = new Random();
        return Optional.of(inactiveNpcs.get(random.nextInt(inactiveNpcs.size())));
    }

    public static Map<String, Object> getNpcData(String npcName) {
        return npcDataMap.getOrDefault(npcName, null);
    }

    public static void activateNpc(String npcName) {
        if (inactiveNpcs.remove(npcName)) {
            activeNpcs.add(npcName);
            LOGGER.info("PNJ activé : " + npcName);
        }
    }

    public static void deactivateNpc(String npcName) {
        if (activeNpcs.remove(npcName)) {
            inactiveNpcs.add(npcName);
            LOGGER.info("PNJ désactivé : " + npcName);
        }
    }

    public static Set<String> getActiveNpcs() {
        return Collections.unmodifiableSet(activeNpcs);
    }

    public static List<String> getInactiveNpcs() {
        return Collections.unmodifiableList(inactiveNpcs);
    }
    public static boolean isActive(UUID uuid) {
        return activeNpcs.contains(uuid);
    }

    public static void unregisterNpc(TradeNpcEntity npc) {
        activeNpcs.remove(npc.getUUID());
        LOGGER.info("Unregistered NPC: {}", npc.getUUID());
    }
}

