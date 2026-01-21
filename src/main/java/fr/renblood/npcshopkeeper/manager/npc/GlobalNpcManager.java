package fr.renblood.npcshopkeeper.manager.npc;

import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static fr.renblood.npcshopkeeper.manager.npc.ActiveNpcManager.addActiveNpc;

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

        JsonObject root = JsonFileManager.readJsonFile(JsonFileManager.pathConstant);
        if (!root.has("npcs") || !root.get("npcs").isJsonObject()) {
            LOGGER.error("Aucune clé 'npcs' valide dans : " + JsonFileManager.pathConstant);
            return;
        }

        JsonObject npcsObj = root.getAsJsonObject("npcs");
        for (Map.Entry<String, JsonElement> entry : npcsObj.entrySet()) {
            String name = entry.getKey();
            JsonObject o = entry.getValue().getAsJsonObject();

            String texture = o.has("Texture")
                    ? o.get("Texture").getAsString()
                    : "textures/entity/banker.png";
            
            boolean isShopkeeper = o.has("IsShopkeeper") && o.get("IsShopkeeper").getAsBoolean();
            boolean isCreatedByPlayer = o.has("IsCreatedByPlayer") && o.get("IsCreatedByPlayer").getAsBoolean();

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
            data.put("IsShopkeeper", isShopkeeper);
            data.put("IsCreatedByPlayer", isCreatedByPlayer);

            npcDataMap.put(name, data);
            
            // Seuls les Shopkeepers sont ajoutés à la liste des PNJs disponibles pour les routes
            if (isShopkeeper) {
                inactiveNpcs.add(name);
            }
        }

        LOGGER.info("Données PNJ chargées avec succès. Total PNJs : " + npcDataMap.size() + " (dont " + inactiveNpcs.size() + " shopkeepers)");
    }
    
    public static boolean registerNewNpc(String name, String texture, boolean isShopkeeper, List<String> texts) {
        if (npcDataMap.containsKey(name)) {
            LOGGER.warn("Tentative de création d'un PNJ existant : " + name);
            return false;
        }

        // 1. Mise à jour de la mémoire
        Map<String, Object> data = new HashMap<>();
        data.put("Texture", texture);
        data.put("Texts", texts != null ? texts : new ArrayList<String>());
        data.put("IsShopkeeper", isShopkeeper);
        data.put("IsCreatedByPlayer", true); // Marqué comme créé par le joueur
        
        npcDataMap.put(name, data);
        if (isShopkeeper) {
            inactiveNpcs.add(name);
        }

        // 2. Sauvegarde dans constant.json
        JsonObject root = JsonFileManager.readJsonFile(JsonFileManager.pathConstant);
        if (root == null) root = new JsonObject();
        
        if (!root.has("npcs")) {
            root.add("npcs", new JsonObject());
        }
        JsonObject npcsObj = root.getAsJsonObject("npcs");
        
        JsonObject newNpc = new JsonObject();
        newNpc.addProperty("Texture", texture);
        newNpc.addProperty("IsShopkeeper", isShopkeeper);
        newNpc.addProperty("IsCreatedByPlayer", true);
        
        JsonArray textsArray = new JsonArray();
        if (texts != null) {
            for (String t : texts) {
                if (t != null && !t.isEmpty()) {
                    textsArray.add(t);
                }
            }
        }
        newNpc.add("Texts", textsArray);
        
        npcsObj.add(name, newNpc);
        
        try (FileWriter writer = new FileWriter(JsonFileManager.pathConstant)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(root, writer);
            LOGGER.info("Nouveau PNJ sauvegardé : " + name);
            return true;
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la sauvegarde du nouveau PNJ", e);
            return false;
        }
    }

    public static void activateNpc(TradeNpc npc) {
        if (inactiveNpcs.remove(npc.getNpcName())) {
            activeNpcs.add(npc.getNpcName());
            LOGGER.info("PNJ activé : " + npc.getNpcName());
            addActiveNpc(npc);
        }
    }

    public static void deactivateNpc(String npcName) {
        // On ne remet dans la liste inactive que si c'est un shopkeeper connu
        Map<String, Object> data = npcDataMap.get(npcName);
        boolean isShopkeeper = data != null && (boolean) data.getOrDefault("IsShopkeeper", true); // true par défaut pour compatibilité

        if (activeNpcs.remove(npcName)) {
            if (isShopkeeper) {
                inactiveNpcs.add(npcName);
            }
            LOGGER.info("PNJ désactivé : " + npcName);
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
}
