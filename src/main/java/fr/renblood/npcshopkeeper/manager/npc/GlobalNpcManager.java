package fr.renblood.npcshopkeeper.manager.npc;

import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static fr.renblood.npcshopkeeper.manager.npc.ActiveNpcManager.addActiveNpc;
import static fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.PATH_CONSTANT;

import com.google.gson.*;

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
        inactiveNpcs.clear();
        activeNpcs.clear();

        JsonObject root = JsonFileManager.readJsonFile(PATH_CONSTANT);
        if (!root.has("npcs") || !root.get("npcs").isJsonObject()) {
            LOGGER.error("Aucune clé 'npcs' valide dans : " + PATH_CONSTANT);
            return;
        }

        JsonObject npcsObj = root.getAsJsonObject("npcs");
        for (Map.Entry<String, JsonElement> entry : npcsObj.entrySet()) {
            String name = entry.getKey();
            JsonObject o = entry.getValue().getAsJsonObject();

            String texture = o.has("Texture")
                    ? o.get("Texture").getAsString()
                    : "textures/entity/banker.png";

            List<String> texts = new ArrayList<>();
            if (o.has("Texts") && o.get("Texts").isJsonArray()) {
                for (JsonElement t : o.getAsJsonArray("Texts")) {
                    if (t.isJsonPrimitive()) {
                        texts.add(t.getAsString());
                    }
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("Texture", texture);
            data.put("Texts", texts);

            npcDataMap.put(name, data);
            inactiveNpcs.add(name);
        }

        LOGGER.info("Données PNJ chargées avec succès. Total PNJs : " + npcDataMap.size());
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
