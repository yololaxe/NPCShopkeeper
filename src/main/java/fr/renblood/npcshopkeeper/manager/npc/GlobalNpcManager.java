package fr.renblood.npcshopkeeper.manager.npc;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.api.NpcTemplate;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
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
    private static final Map<String, Map<String, Object>> npcDataById = new HashMap<>();
    private static final Map<String, String> npcNameById = new HashMap<>();

    public static int getDataSize() {
        return npcDataMap.size();
    }

    // Charger les données du fichier JSON au démarrage
    public static void loadNpcData() {
        npcDataMap.clear();
        npcDataById.clear();
        npcNameById.clear();
        inactiveNpcs.clear();
        activeNpcs.clear();

        // Utilisation de OnServerStartedManager.PATH_CONSTANT au lieu de JsonFileManager.pathConstant
        // car JsonFileManager.pathConstant peut être null si initialisé trop tôt
        String path = OnServerStartedManager.PATH_CONSTANT;
        if (path == null) {
            LOGGER.error("Chemin constant.json non initialisé !");
            return;
        }

        JsonObject root = JsonFileManager.readJsonFile(path);
        if (!root.has("npcs") || !root.get("npcs").isJsonObject()) {
            LOGGER.error("Aucune clé 'npcs' valide dans : " + path);
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
            if (o.has("npc_id")) data.put("npcId", o.get("npc_id").getAsString());
            else if (o.has("npcId")) data.put("npcId", o.get("npcId").getAsString());

            npcDataMap.put(name, data);
            indexById(name, data);
            
            // Seuls les Shopkeepers sont ajoutés à la liste des PNJs disponibles pour les routes
            if (isShopkeeper) {
                inactiveNpcs.add(name);
            }
        }

        Npcshopkeeper.debugLog(LOGGER, "Données PNJ chargées avec succès. Total PNJs : " + npcDataMap.size() + " (dont " + inactiveNpcs.size() + " shopkeepers)");
    }
    
    // Méthode pour ajouter un PNJ depuis un template API
    public static void addNpcFromTemplate(NpcTemplate template) {
        if (template == null || template.name == null || template.name.isBlank()) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("npcId", template.npc_id);
        data.put("NpcType", template.type);
        data.put("Texture", template.texture);
        data.put("Texts", template.dialogue != null ? template.dialogue : new ArrayList<String>());
        boolean isShopkeeper = "SHOPKEEPER".equals(template.type);
        data.put("IsShopkeeper", isShopkeeper);
        data.put("IsCreatedByPlayer", true);
        data.put("TradeCategory", template.trade_category);
        data.put("QuestLinks", template.quest_links != null ? template.quest_links : List.of());
        data.put("Implementation", template.implementation != null ? template.implementation : Map.of());
        
        // On utilise le nom comme clé pour l'instant (compatibilité)
        // Idéalement, on devrait utiliser npc_id, mais le reste du code utilise le nom.
        npcDataMap.put(template.name, data);
        indexById(template.name, data);
        
        if (isShopkeeper) {
            if (!activeNpcs.contains(template.name) && !inactiveNpcs.contains(template.name)) {
                inactiveNpcs.add(template.name);
            }
        }
        
        Npcshopkeeper.debugLog(LOGGER, "PNJ ajouté depuis template API : " + template.name);
    }
    
    public static Map<String, Object> getNpcDataById(String npcId) {
        if (npcId == null) return null;
        return npcDataById.get(npcId);
    }

    public static String getNpcNameById(String npcId) {
        if (npcId == null) return null;
        return npcNameById.get(npcId);
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
        indexById(name, data);
        if (isShopkeeper) {
            inactiveNpcs.add(name);
        }

        // 2. Sauvegarde dans constant.json
        String path = OnServerStartedManager.PATH_CONSTANT;
        if (path == null) {
            LOGGER.error("Impossible de sauvegarder le PNJ : chemin constant.json non initialisé.");
            return false;
        }

        JsonObject root = JsonFileManager.readJsonFile(path);
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
        
        try (FileWriter writer = new FileWriter(path)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(root, writer);
            Npcshopkeeper.debugLog(LOGGER, "Nouveau PNJ sauvegardé : " + name);
            return true;
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la sauvegarde du nouveau PNJ", e);
            return false;
        }
    }

    public static void activateNpc(TradeNpc npc) {
        if (inactiveNpcs.remove(npc.getNpcName())) {
            activeNpcs.add(npc.getNpcName());
            Npcshopkeeper.debugLog(LOGGER, "PNJ activé : " + npc.getNpcName());
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
            Npcshopkeeper.debugLog(LOGGER, "PNJ désactivé : " + npcName);
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

        Npcshopkeeper.debugLog(LOGGER, "PNJ inactif aléatoire sélectionné : " + randomNpcName);
        return randomNpcName;
    }

    public static Map<String, Object> getNpcData(String npcName) {
        if (npcName == null) return null;
        String normalized = npcName.replace('_', ' ');
        Map<String, Object> direct = npcDataMap.get(normalized);
        if (direct != null) return direct;
        return npcDataMap.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(normalized))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
        }

        public static String resolveNpcName(String requestedName) {
        if (requestedName == null) return null;
        String normalized = requestedName.replace('_', ' ');
        return npcDataMap.keySet().stream()
            .filter(name -> name.equalsIgnoreCase(normalized))
            .findFirst()
            .orElse(null);
    }
    
    // Méthode pour récupérer toutes les données (pour l'envoi au client)
    public static Map<String, Map<String, Object>> getAllNpcData() {
        return Collections.unmodifiableMap(npcDataMap);
    }
    
    // Méthode pour recevoir les données côté client
    public static void setClientData(Map<String, Map<String, Object>> data) {
        npcDataMap.clear();
        npcDataById.clear();
        npcNameById.clear();
        npcDataMap.putAll(data);
        npcDataMap.forEach(GlobalNpcManager::indexById);
        Npcshopkeeper.debugLog(LOGGER, "Données globales des PNJs synchronisées côté client. Total : " + npcDataMap.size());
    }

    private static void indexById(String name, Map<String, Object> data) {
        Object id = data.get("npcId");
        if (id instanceof String npcId && !npcId.isBlank()) {
            npcDataById.put(npcId, data);
            npcNameById.put(npcId, name);
        }
    }

    private static void indexById(Map.Entry<String, Map<String, Object>> entry) {
        indexById(entry.getKey(), entry.getValue());
    }

    public static Set<String> getActiveNpcs() {
        return Collections.unmodifiableSet(activeNpcs);
    }

    public static List<String> getInactiveNpcs() {
        return Collections.unmodifiableList(inactiveNpcs);
    }
}
