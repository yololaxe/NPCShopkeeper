package fr.renblood.npcshopkeeper.manager.trade;

import com.google.gson.*;
import com.ibm.icu.impl.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.renblood.npcshopkeeper.data.io.JsonFileManager.readJsonFile;
import static fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.PATH_PRICE;

public class PriceReferenceManager {

    private static final Logger LOGGER = LogManager.getLogger(PriceReferenceManager.class);

    public static String pathPrice = PATH_PRICE;

    // Cache pour éviter de lire le fichier à chaque appel
    private static final Map<String, Pair<Integer, Integer>> priceCache = new HashMap<>();
    private static long lastCacheUpdate = 0;

    // Écrire dans le fichier JSON
    public static void writeJsonFile(Path filePath, JsonObject jsonObject) {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(jsonObject.toString());
            // Invalider le cache après écriture
            lastCacheUpdate = 0;
        } catch (IOException e) {
            LOGGER.error("Erreur lors de l'écriture du fichier JSON : " + e.getMessage());
        }
    }

    private static void refreshCache() {
        File file = new File(pathPrice);
        if (!file.exists()) return;

        // Si le fichier n'a pas changé depuis la dernière mise à jour, on ne fait rien
        if (file.lastModified() <= lastCacheUpdate) return;

        // LOGGER.info("Refreshing price reference cache...");
        priceCache.clear();
        JsonObject jsonObject = readJsonFile(pathPrice);

        if (jsonObject != null && jsonObject.has("references")) {
            JsonArray referencesArray = jsonObject.getAsJsonArray("references");
            for (JsonElement element : referencesArray) {
                JsonObject reference = element.getAsJsonObject();
                String item = reference.get("item").getAsString();
                int min = reference.get("min").getAsInt();
                int max = reference.get("max").getAsInt();
                priceCache.put(item, Pair.of(min, max));
            }
        }
        lastCacheUpdate = file.lastModified();
    }

    // Chercher une référence par nom d'item
    public static Pair<Integer, Integer> findReferenceByItem(String itemName, Player player) {
        refreshCache();
        return priceCache.getOrDefault(itemName, Pair.of(1, 1));
    }

    // Créer une nouvelle référence
    public static boolean createPriceReference(String item, int min, int max, Player player) {
        try {
            String path = player.getServer().getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/price_references.json").toString();
            JsonObject jsonObject = readJsonFile(path);
            if (jsonObject == null) jsonObject = new JsonObject();

            JsonArray referencesArray = jsonObject.has("references") ? jsonObject.getAsJsonArray("references") : new JsonArray();

            // Créer un nouvel objet JSON pour la référence de prix
            JsonObject reference = new JsonObject();
            reference.addProperty("item", item);
            reference.addProperty("min", min);
            reference.addProperty("max", max);

            referencesArray.add(reference);
            jsonObject.add("references", referencesArray);

            // Vérifier et créer le répertoire parent si nécessaire
            File file = new File(path);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs(); // Crée le répertoire parent s'il n'existe pas
            }

            // Sauvegarder le fichier
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonObject.toString());
            }

            // Invalider le cache
            lastCacheUpdate = 0;

            LOGGER.info("Référence de prix ajoutée pour l'item : " + item);
            player.displayClientMessage(Component.literal("Référence de prix ajoutée pour l'item : " + item + " min : "+min+" max : "+max), false);

            return true;

        } catch (IOException e) {
            LOGGER.error("Erreur lors de la création de la référence de prix", e);
            return false;
        }
    }

    // Modifier une référence existante
    public static void updateReference(String itemName, int newMinPrice, int newMaxPrice) {
        JsonObject jsonObject = readJsonFile(pathPrice);
        if (jsonObject == null) return;

        if (jsonObject.has("references")) {
            JsonArray referencesArray = jsonObject.getAsJsonArray("references");
            boolean found = false;
            for (JsonElement element : referencesArray) {
                JsonObject reference = element.getAsJsonObject();
                if (reference.get("item").getAsString().equals(itemName)) {
                    reference.addProperty("min", newMinPrice);
                    reference.addProperty("max", newMaxPrice);
                    found = true;
                    break;
                }
            }
            if (found) {
                writeJsonFile(Path.of(pathPrice), jsonObject);
                LOGGER.info("Référence mise à jour : " + itemName);
            } else {
                LOGGER.warn("Référence non trouvée pour l'item : " + itemName);
            }
        }
    }

    // Récupérer toutes les références sous forme de liste
    public static List<JsonObject> getAllReferences() {
        List<JsonObject> references = new ArrayList<>();
        JsonObject jsonObject = readJsonFile(pathPrice);

        if (jsonObject != null && jsonObject.has("references")) {
            JsonArray referencesArray = jsonObject.getAsJsonArray("references");
            for (JsonElement element : referencesArray) {
                references.add(element.getAsJsonObject());
            }
        }
        return references;
    }
}
