package fr.renblood.npcshopkeeper.manager.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static fr.renblood.npcshopkeeper.data.io.JsonFileManager.readJsonFile;
import static fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.PATH_XP;

public class XpReferenceManager {

    private static final Logger LOGGER = LogManager.getLogger(XpReferenceManager.class);
    private static final Random RANDOM = new Random();

    // Cache : Item -> XpInfo (Job, Min, Max)
    private static final Map<String, XpInfo> xpCache = new HashMap<>();
    private static long lastCacheUpdate = 0;

    public static class XpInfo {
        public final String job;
        public final float min;
        public final float max;

        public XpInfo(String job, float min, float max) {
            this.job = job;
            this.min = min;
            this.max = max;
        }
        
        public float getRandomXp() {
            if (min >= max) return min;
            return min + RANDOM.nextFloat() * (max - min);
        }
        
        public float getAverageXp() {
            return (min + max) / 2.0f;
        }
    }

    private static void refreshCache() {
        File file = new File(PATH_XP);
        if (!file.exists()) return;

        if (file.lastModified() <= lastCacheUpdate) return;

        xpCache.clear();
        JsonObject jsonObject = readJsonFile(PATH_XP);

        if (jsonObject != null && jsonObject.has("references")) {
            JsonArray referencesArray = jsonObject.getAsJsonArray("references");
            for (JsonElement element : referencesArray) {
                JsonObject reference = element.getAsJsonObject();
                String item = reference.get("item").getAsString();
                String job = reference.get("job").getAsString();
                
                float min, max;
                if (reference.has("xp")) { // Rétrocompatibilité
                    min = max = reference.get("xp").getAsFloat();
                } else {
                    min = reference.get("min").getAsFloat();
                    max = reference.get("max").getAsFloat();
                }
                
                XpInfo info = new XpInfo(job, min, max);
                xpCache.put(item, info);
                xpCache.put(normalizeItemId(item), info);
            }
        }
        lastCacheUpdate = file.lastModified();
        Npcshopkeeper.debugLog(LOGGER, "XP Reference Cache refreshed. Size: " + xpCache.size());
    }

    // Chercher une référence XP par nom d'item
    public static XpInfo getXpReference(String itemName) {
        var apiReference = MedievalCoinsIntegration.getReferenceXp(null, itemName);
        if (apiReference.isEmpty()) {
            apiReference = MedievalCoinsIntegration.getReferenceXp(null, normalizeItemId(itemName));
        }

        if (apiReference.isPresent()) {
            MedievalCoinsIntegration.XpReferenceInfo reference = apiReference.get();
            return new XpInfo(reference.job(), reference.amount(), reference.amount());
        }

        refreshCache();
        XpInfo info = xpCache.get(itemName);
        return info != null ? info : xpCache.get(normalizeItemId(itemName));
    }

    private static String normalizeItemId(String itemName) {
        if (itemName == null) return "";
        String normalized = itemName.toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    // Créer ou mettre à jour une référence XP
    public static boolean createOrUpdateXpReference(String item, String job, float min, float max) {
        try {
            JsonObject jsonObject = readJsonFile(PATH_XP);
            if (jsonObject == null) jsonObject = new JsonObject();

            JsonArray referencesArray = jsonObject.has("references") ? jsonObject.getAsJsonArray("references") : new JsonArray();
            boolean found = false;

            // Vérifier si existe déjà et mettre à jour
            for (JsonElement el : referencesArray) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.get("item").getAsString().equals(item)) {
                    obj.addProperty("job", job);
                    obj.addProperty("min", min);
                    obj.addProperty("max", max);
                    if (obj.has("xp")) obj.remove("xp"); // Nettoyage ancien format
                    found = true;
                    break;
                }
            }

            if (!found) {
                JsonObject reference = new JsonObject();
                reference.addProperty("item", item);
                reference.addProperty("job", job);
                reference.addProperty("min", min);
                reference.addProperty("max", max);
                referencesArray.add(reference);
            }

            jsonObject.add("references", referencesArray);

            File file = new File(PATH_XP);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonObject.toString());
            }

            lastCacheUpdate = 0; // Invalider cache
            LOGGER.info("Référence XP {} : {} -> {} ({} - {} xp)", found ? "mise à jour" : "ajoutée", item, job, min, max);
            return true;

        } catch (IOException e) {
            LOGGER.error("Erreur lors de la création de la référence XP", e);
            return false;
        }
    }
}
