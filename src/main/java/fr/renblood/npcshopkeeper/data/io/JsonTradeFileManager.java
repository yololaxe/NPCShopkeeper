package fr.renblood.npcshopkeeper.data.io;

import com.google.gson.*;
import com.ibm.icu.impl.Pair;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.data.trade.TradeItem;
import fr.renblood.npcshopkeeper.data.trade.TradeResult;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

import static fr.renblood.npcshopkeeper.data.commercial.CommercialRoad.updateCommercialRoadAfterRemoval;
import static fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.*;

public class JsonTradeFileManager {

    private static final Logger LOGGER = LogManager.getLogger(JsonTradeFileManager.class);
    public static String path = PATH;
    public static String pathHistory = PATH_HISTORY;
    public static String pathConstant = PATH_CONSTANT;
    public static String pathCommercial = PATH_COMMERCIAL;
    public static String pathNpcs = PATH_NPCS;

    // Méthode pour lire un fichier JSON
    public static JsonObject readJsonFile(String read_path) {
        Path filePath = Path.of(read_path);
        try {
            // Vérifier si le fichier existe
            if (!Files.exists(filePath)) {
                LOGGER.error("Le fichier JSON n'existe pas : {}", filePath);
                return new JsonObject(); // Retourne un objet vide
            }

            // Lire le contenu du fichier
            String content = Files.readString(filePath);
            JsonElement jsonElement = JsonParser.parseString(content);

            // Vérifier que le contenu est un objet JSON valide
            if (!jsonElement.isJsonObject()) {
                LOGGER.error("Le contenu du fichier JSON n'est pas un objet JSON valide : {}", filePath);
                return new JsonObject(); // Retourne un objet vide
            }

            return jsonElement.getAsJsonObject();
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la lecture du fichier JSON : {}", filePath, e);
            return new JsonObject(); // Retourne un objet vide si le fichier ne peut pas être lu
        } catch (Exception e) {
            LOGGER.error("Erreur inattendue lors de la lecture du fichier JSON : {}", filePath, e);
            return new JsonObject(); // Retourne un objet vide en cas d'erreur inattendue
        }
    }

    // Méthode pour écrire dans un fichier JSON
    private static void writeJsonFile(String write_path, JsonObject jsonObject) {
        Path filePath = Path.of(write_path);
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            writer.write(gson.toJson(jsonObject));
        } catch (IOException e) {
            LOGGER.error("Erreur lors de l'écriture du fichier JSON : " + e.getMessage());
        }
    }


    private static <T> List<T> extractFromTrades(String field, Function<JsonObject, T> extractor) {
        List<T> result = new ArrayList<>();
        JsonObject jsonObject = readJsonFile(path);

        if (jsonObject.has("trades")) {
            JsonArray tradeArray = jsonObject.getAsJsonArray("trades");
            for (JsonElement tradeElement : tradeArray) {
                JsonObject tradeObject = tradeElement.getAsJsonObject();
                if (tradeObject.has(field)) {
                    result.add(extractor.apply(tradeObject));
                }
            }
        }

        return result;
    }
    public static List<String> readTradeNames() {
        return extractFromTrades("Name", obj -> obj.get("Name").getAsString());
    }

    public static Set<String> getAllCategory() {
        return new HashSet<>(extractFromTrades("Category", obj -> obj.get("Category").getAsString()));
    }

    public static List<String> readCategoryNames() {
        return extractFromTrades("Category", obj -> obj.get("Category").getAsString())
                .stream().distinct().toList();
    }

    // Récupérer un trade spécifique par son nom
    public static Trade getTradeByName(String tradeName) {
        JsonObject jsonObject = readJsonFile(path);

        if (jsonObject.has("trades")) {
            JsonArray tradeArray = jsonObject.getAsJsonArray("trades");
            for (JsonElement tradeElement : tradeArray) {
                JsonObject tradeObject = tradeElement.getAsJsonObject();
                String name = tradeObject.get("Name").getAsString();
                if (name.equalsIgnoreCase(tradeName)) {
                    String category = tradeObject.get("Category").getAsString();
                    List<TradeItem> tradeItems = new ArrayList<>();
                    JsonArray tradesArray = tradeObject.getAsJsonArray("trades");
                    tradesArray.forEach(itemElement -> {
                        JsonObject itemObject = itemElement.getAsJsonObject();
                        tradeItems.add(new TradeItem(itemObject.get("item").getAsString(),
                                itemObject.get("min").getAsInt(),
                                itemObject.get("max").getAsInt()));
                    });
                    TradeResult tradeResult = null;
                    if (tradeObject.has("result") && tradeObject.get("result").isJsonObject()) {
                        JsonObject resultObject = tradeObject.getAsJsonObject("result");
                        tradeResult = new TradeResult(resultObject.get("item").getAsString(),
                                resultObject.get("quantity").getAsInt());
                    }
                    LOGGER.info("Trade trouvé : " + name);
                    return new Trade(name, category, tradeItems, tradeResult);
                }
            }
        }
        LOGGER.warn("Aucun trade avec le nom " + tradeName + " n'a été trouvé.");
        return null;
    }

    // Méthode pour enregistrer le début d'un trade


    private static boolean containsPlayer(JsonArray playersArray, String playerName) {
        for (JsonElement element : playersArray) {
            if (element.getAsString().equals(playerName)) {
                return true;
            }
        }
        return false;
    }
    // Méthode pour supprimer le PNJ associé au trade
    public static void removeNpc(ServerPlayer player, String id) {
        ServerLevel level = player.serverLevel();
        UUID entityUuid = UUID.fromString(id);
        Entity entity = level.getEntity(entityUuid);

        if (entity != null) {
            if (entity instanceof TradeNpcEntity tradeNpcEntity) {
                tradeNpcEntity.remove(Entity.RemovalReason.DISCARDED);
                LOGGER.info("NPC avec l'UUID : " + id + " supprimé avec succès.");

                // ➕ Supprimer ce PNJ de la route dans le fichier commercial
                JsonTradeFileManager.removeNpcFromRoadJson(entityUuid);

                // ➕ Mettre à jour la route si besoin
                updateCommercialRoadAfterRemoval(tradeNpcEntity);
            } else {
                LOGGER.warn("L'entité avec l'UUID : " + id + " n'est pas une instance de TradeNpcEntity.");
            }
        } else {
            LOGGER.warn("Aucune entité trouvée avec l'UUID : " + id + ".");
        }
    }



    public static List<Trade> getTradesByCategory(String category) {
        List<Trade> trades = new ArrayList<>();
        JsonObject jsonObject = readJsonFile(path);

        if (jsonObject.has("trades")) {
            JsonArray tradeArray = jsonObject.getAsJsonArray("trades");
            tradeArray.forEach(tradeElement -> {
                JsonObject tradeObject = tradeElement.getAsJsonObject();

                // Vérification que la catégorie correspond
                if (tradeObject.has("Category") && tradeObject.get("Category").getAsString().equals(category)) {
                    String name = tradeObject.has("Name") ? tradeObject.get("Name").getAsString() : "Unknown";

                    // Récupération des items de trade
                    List<TradeItem> tradeItems = new ArrayList<>();
                    if (tradeObject.has("trades")) {
                        JsonArray tradesArray = tradeObject.getAsJsonArray("trades");
                        tradesArray.forEach(itemElement -> {
                            JsonObject itemObject = itemElement.getAsJsonObject();
                            String item = itemObject.has("item") ? itemObject.get("item").getAsString() : "minecraft:air";
                            int min = itemObject.has("min") ? itemObject.get("min").getAsInt() : 0;
                            int max = itemObject.has("max") ? itemObject.get("max").getAsInt() : 0;
                            tradeItems.add(new TradeItem(item, min, max));
                        });
                    }

                    // Récupération du résultat du trade (peut être null)
                    TradeResult tradeResult = null;
                    if (tradeObject.has("result") && !tradeObject.get("result").isJsonNull()) {
                        JsonObject resultObject = tradeObject.getAsJsonObject("result");
                        String resultItem = resultObject.has("item") ? resultObject.get("item").getAsString() : "minecraft:air";
                        int quantity = resultObject.has("quantity") ? resultObject.get("quantity").getAsInt() : 1;
                        tradeResult = new TradeResult(resultItem, quantity);
                    }

                    // Ajout du trade à la liste
                    trades.add(new Trade(name, category, tradeItems, tradeResult));
                }
            });
        }
        return trades;
    }
    /////////////////////////////////////////////TRADES HISTORY/////////////////////////////////////////////

    public static void logTradeStart(ServerPlayer player, String tradeName, String id, List<Map<String, Object>> tradeItems, int totalPrice, List<Player> players, String npcId, String npcName) {
        //Path path = Paths.get(player.getServer().getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade_history.json").toString());
        JsonObject jsonObject = readJsonFile(pathHistory);

        JsonArray historyArray = jsonObject.has("history") ? jsonObject.getAsJsonArray("history") : new JsonArray();
        boolean tradeExists = false;

        // Parcourir l'historique pour vérifier s'il y a un trade non terminé pour ce NPC
        for (JsonElement element : historyArray) {
            JsonObject tradeEntry = element.getAsJsonObject();

            String tradeNpcId = tradeEntry.get("npcId").getAsString();
            boolean isFinished = tradeEntry.get("isFinished").getAsBoolean();

            // Si un trade non terminé avec le même NPC existe, ajouter les joueurs à ce trade
            if (tradeNpcId.equals(npcId) && !isFinished) {
                tradeExists = true;

                // Ajouter les nouveaux joueurs à la liste des joueurs existants
                JsonArray existingPlayersArray = tradeEntry.getAsJsonArray("players");
                for (Player player1 : players) {
                    String playerName = player1.getName().getString();
                    if (!containsPlayer(existingPlayersArray, playerName)) {
                        existingPlayersArray.add(playerName);
                    }
                }

                // Mettre à jour le trade dans l'historique
                tradeEntry.add("players", existingPlayersArray);
                break;
            }
        }

        // Si aucun trade non terminé trouvé, créer une nouvelle entrée
        if (!tradeExists) {
            JsonArray playersArray = new JsonArray();
            for (Player player1 : players) {
                playersArray.add(player1.getName().getString());
            }

            JsonObject newTradeEntry = new JsonObject();
            newTradeEntry.addProperty("id", id);
            newTradeEntry.add("players", playersArray);
            newTradeEntry.addProperty("tradeName", tradeName);
            newTradeEntry.addProperty("isFinished", false);
            newTradeEntry.addProperty("npcId", npcId);
            newTradeEntry.addProperty("npcName", npcName);

            // Ajouter la date et l'heure du trade
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            newTradeEntry.addProperty("dateTime", formattedDateTime);

            // Ajouter le total des trades
            newTradeEntry.addProperty("totalPrice", totalPrice);

            // Ajouter les items du trade
            JsonArray tradesArray = new JsonArray();
            for (Map<String, Object> tradeItem : tradeItems) {
                JsonObject tradeItemObject = new JsonObject();
                tradeItemObject.addProperty("item", (String) tradeItem.get("item"));
                tradeItemObject.addProperty("quantity", (int) tradeItem.get("quantity"));
                tradeItemObject.addProperty("price", (int) tradeItem.get("price"));
                tradesArray.add(tradeItemObject);
            }
            newTradeEntry.add("trades", tradesArray);

            // Ajouter cette nouvelle entrée dans l'historique
            historyArray.add(newTradeEntry);
        }

        // Sauvegarder le fichier JSON
        jsonObject.add("history", historyArray);
        writeJsonFile((pathHistory), jsonObject);

        if (tradeExists) {
            LOGGER.info("Un trade non terminé avec le NPC " + npcName + " a été mis à jour avec de nouveaux joueurs.");
        } else {
            LOGGER.info("Un nouveau trade loggué pour le joueur " + player.getName().getString() + " et le NPC " + npcName);
        }
    }
    // Méthode pour marquer un trade comme terminé
    public static void markTradeAsFinished(ServerPlayer player, String id) {
        // Obtenir le chemin du fichier trade_history.json
        //Path path = Paths.get(player.getServer().getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade_history.json").toString());
        JsonObject jsonObject = readJsonFile(pathHistory);

        // Vérifier l'historique des trades
        JsonArray historyArray = jsonObject.getAsJsonArray("history");
        for (JsonElement tradeElement : historyArray) {
            JsonObject tradeObject = tradeElement.getAsJsonObject();

            // Vérifier si le trade correspond à l'ID et n'est pas terminé
            if (tradeObject.get("id").getAsString().equals(id) &&
                    tradeObject.get("isFinished").getAsString().equals("false")) {

                // Marquer le trade comme terminé
                tradeObject.addProperty("isFinished", true);
                writeJsonFile((pathHistory), jsonObject);
                LOGGER.info("Trade marqué comme terminé pour " + player.getName().getString() + " et le trade " + id);

                // Obtenir le npcId associé au trade
                String npcId = tradeObject.get("npcId").getAsString();

                // Faire disparaître le PNJ
                removeNpc(player, npcId); // Remplacez id par npcId ici
                return;
            }
        }
        LOGGER.warn("Aucune entrée trouvée pour le joueur " + player.getName().getString() + " et le trade " + id);
    }
    public static TradeHistory getTradeHistoryById(String id) {
        JsonObject jsonObject = readJsonFile(pathHistory);

        if (jsonObject.has("history")) {
            JsonArray historyArray = jsonObject.getAsJsonArray("history");
            for (JsonElement tradeElement : historyArray) {
                JsonObject tradeObject = tradeElement.getAsJsonObject();
                String tradeId = tradeObject.get("id").getAsString();

                if (tradeId.equals(id)) {
                    List<String> playersName = new ArrayList<>();
                    if (tradeObject.has("player")) {
                        JsonArray playersArray = tradeObject.getAsJsonArray("player");
                        for (JsonElement playerElement : playersArray) {
                            playersName.add(playerElement.getAsString());
                        }
                    }
                    String tradeName = tradeObject.get("tradeName").getAsString();
                    boolean isFinished = tradeObject.get("isFinished").getAsBoolean();
                    String npcId = tradeObject.get("npcId").getAsString();
                    String npcName = tradeObject.get("npcName").getAsString();

                    // Récupération des tradeItems
                    List<Map<String, Object>> tradeItems = new ArrayList<>();
                    if (tradeObject.has("trades")) {
                        JsonArray tradesArray = tradeObject.getAsJsonArray("trades");

                        for (JsonElement tradeItemElement : tradesArray) {
                            JsonObject tradeItemObject = tradeItemElement.getAsJsonObject();
                            Map<String, Object> tradeItemMap = new HashMap<>();

                            String item = tradeItemObject.get("item").getAsString();
                            int quantity = tradeItemObject.get("quantity").getAsInt();
                            int price = tradeItemObject.get("price").getAsInt();

                            tradeItemMap.put("item", item);
                            tradeItemMap.put("quantity", quantity);
                            tradeItemMap.put("price", price);

                            tradeItems.add(tradeItemMap);
                        }
                    }

                    // Récupération du totalPrice
                    int totalPrice = tradeObject.has("totalPrice") ? tradeObject.get("totalPrice").getAsInt() : 0;

                    // Retourne un TradeHistory avec les informations du joueur, du trade, du statut et du prix total
                    return new TradeHistory(playersName, tradeName, isFinished, tradeId, tradeItems, totalPrice, npcId, npcName);
                }
            }
        }

        LOGGER.warn("Aucun trade avec l'ID " + id + " n'a été trouvé.");
        return null;
    }

    public static Pair<Boolean, TradeHistory> checkTradeStatusForNpc(String npcId) {
        LOGGER.info("Début de la vérification du statut du trade pour le NPC : " + npcId);
        JsonObject jsonObject = readJsonFile(pathHistory);

        if (jsonObject.has("history")) {
            JsonArray historyArray = jsonObject.getAsJsonArray("history");
            LOGGER.info("Nombre de trades dans l'historique : " + historyArray.size());

            for (JsonElement element : historyArray) {
                JsonObject tradeObject = element.getAsJsonObject();
                String currentNpcId = tradeObject.get("npcId").getAsString();
                boolean isFinished = tradeObject.get("isFinished").getAsBoolean();
                LOGGER.info("Traitement du trade pour NPC ID : " + currentNpcId + ", Terminé : " + isFinished);

                if (currentNpcId.equals(npcId) && !isFinished) {
                    LOGGER.info("Un trade non terminé a été trouvé pour le NPC ID : " + npcId);

                    // Récupération du nom du trade
                    String tradeName = tradeObject.has("tradeName") ? tradeObject.get("tradeName").getAsString() : null;
                    if (tradeName == null) {
                        LOGGER.error("Le nom du trade est null pour le NPC : " + npcId);
                    } else {
                        LOGGER.info("Nom du trade récupéré : " + tradeName);
                    }

                    // Récupération des items du trade
                    List<Map<String, Object>> tradeItems = new ArrayList<>();
                    if (tradeObject.has("trades")) {
                        JsonArray tradesArray = tradeObject.getAsJsonArray("trades");
                        LOGGER.info("Nombre d'items dans le trade : " + tradesArray.size());

                        for (JsonElement tradeItemElement : tradesArray) {
                            JsonObject tradeItemObject = tradeItemElement.getAsJsonObject();
                            Map<String, Object> tradeItem = new HashMap<>();
                            tradeItem.put("item", tradeItemObject.get("item").getAsString());
                            tradeItem.put("quantity", tradeItemObject.get("quantity").getAsInt());
                            tradeItem.put("price", tradeItemObject.get("price").getAsInt());
                            tradeItems.add(tradeItem);
                            LOGGER.info("Item ajouté : " + tradeItem);
                        }
                    } else {
                        LOGGER.warn("Aucun item trouvé pour le trade du NPC : " + npcId);
                    }

                    // Vérification des données critiques
                    if (tradeItems.isEmpty()) {
                        LOGGER.error("Les items du trade sont vides pour le NPC : " + npcId);
                    }

                    int totalPrice = tradeObject.has("totalPrice") ? tradeObject.get("totalPrice").getAsInt() : 0;
                    LOGGER.info("Prix total récupéré pour le trade : " + totalPrice);

                    String npcName = tradeObject.get("npcName").getAsString();
                    LOGGER.info("Nom du NPC récupéré : " + npcName);

                    // Création de l'objet TradeHistory
                    TradeHistory tradeHistory = new TradeHistory(
                            new ArrayList<>(), // Liste des joueurs si nécessaire
                            tradeName,
                            false,
                            tradeObject.get("id").getAsString(),
                            tradeItems,
                            totalPrice,
                            npcId,
                            npcName
                    );
                    LOGGER.info("Objet TradeHistory créé avec succès pour le NPC : " + npcId);
                    return Pair.of(true, tradeHistory);
                }
            }
            LOGGER.info("Aucun trade non terminé trouvé pour le NPC : " + npcId);
        } else {
            LOGGER.warn("Aucun historique de trade trouvé.");
        }

        LOGGER.info("Fin de la vérification du statut du trade pour le NPC : " + npcId);
        TradeHistory tradeHistory = new TradeHistory(new ArrayList<>(), "", true, "", new ArrayList<>(), 0, "", "");
        return Pair.of(false, tradeHistory);
    }
    /////////////////////////////////////////////CONSTANT/////////////////////////////////////////////
    public static Map<String, Map<String, Object>> getPnjData() {
        JsonObject jsonObject = readJsonFile(pathConstant);
        Map<String, Map<String, Object>> npcMap = new HashMap<>();

        if (jsonObject == null || !jsonObject.has("npcs") || !jsonObject.get("npcs").isJsonObject()) {
            LOGGER.error("Le fichier JSON ne contient pas une clé 'npcs' valide dans : " + pathConstant);
            return npcMap;
        }

        JsonObject npcsObject = jsonObject.getAsJsonObject("npcs");

        for (Map.Entry<String, JsonElement> entry : npcsObject.entrySet()) {
            String name = entry.getKey();
            JsonObject npcObject = entry.getValue().getAsJsonObject();

            Map<String, Object> npcData = new HashMap<>();
            String texture = npcObject.has("Texture") ? npcObject.get("Texture").getAsString() : "textures/entity/banker.png";

            List<String> texts = new ArrayList<>();
            if (npcObject.has("Texts") && npcObject.get("Texts").isJsonArray()) {
                JsonArray textsArray = npcObject.get("Texts").getAsJsonArray();
                for (JsonElement textElement : textsArray) {
                    if (textElement.isJsonPrimitive()) {
                        texts.add(textElement.getAsString());
                    }
                }
            }

            npcData.put("Texture", texture);
            npcData.put("Texts", texts);

            npcMap.put(name, npcData);
        }

        LOGGER.info("Données PNJs chargées avec succès depuis le fichier JSON.");
        return npcMap;
    }
    /////////////////////////////////////////////TRADE NPCS/////////////////////////////////////////////
    public static void addTradeNpcToJson(TradeNpc npc) {
        try {
            JsonObject json = readJsonFile(pathNpcs);
            if (!json.has("npcs")) {
                json.add("npcs", new JsonArray());
            }

            JsonArray npcsArray = json.getAsJsonArray("npcs");

            // Vérifier si le NPC existe déjà
            boolean npcExists = false;
            for (JsonElement element : npcsArray) {
                JsonObject npcObject = element.getAsJsonObject();
                if (npcObject.get("id").getAsString().equals(npc.getNpcId())) {
                    npcExists = true;
                    break;
                }
            }

            if (npcExists) {
                LOGGER.warn("Le NPC avec l'ID " + npc.getNpcId() + " est déjà enregistré.");
                return;
            }

            // Ajouter le nouveau NPC
            JsonObject npcObject = new JsonObject();
            npcObject.addProperty("id", npc.getNpcId());
            npcObject.addProperty("name", npc.getNpcName());
            npcObject.addProperty("texture", npc.getTexture());
            npcObject.addProperty("x", npc.getPos().getX());
            npcObject.addProperty("y", npc.getPos().getY());
            npcObject.addProperty("z", npc.getPos().getZ());
            npcObject.addProperty("category", npc.getTradeCategory());

            npcsArray.add(npcObject);
            writeJsonFile((pathNpcs), json);

            LOGGER.info("NPC ajouté avec succès au fichier JSON : " + npc.getNpcId());

            // Activation du PNJ
            GlobalNpcManager.activateNpc(new TradeNpc(npc.getNpcId(), npc.getNpcName(), npc.getNpcData(), npc.getTradeCategory(), npc.getPos()));
            LOGGER.info("PNJ avec ID " + npc.getNpcId() + " activé avec succès.");
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'ajout d'un PNJ dans le fichier JSON : ", e);
        }
    }


    public static void removeTradeNpcFromJson(String npcId) {
        try {
            JsonObject json = readJsonFile(pathNpcs);

            if (!json.has("npcs")) {
                LOGGER.warn("Aucun NPC trouvé pour suppression dans le fichier JSON.");
                return;
            }

            JsonArray npcsArray = json.getAsJsonArray("npcs");
            JsonArray updatedArray = new JsonArray();

            boolean found = false;
            for (JsonElement element : npcsArray) {
                JsonObject npcObject = element.getAsJsonObject();
                if (!npcObject.get("id").getAsString().equals(npcId)) {
                    updatedArray.add(npcObject);
                } else {
                    found = true;
                }
            }

            if (found) {
                json.add("npcs", updatedArray);
                writeJsonFile((pathNpcs), json);
                LOGGER.info("NPC supprimé avec succès du fichier JSON : " + npcId);
            } else {
                LOGGER.warn("Aucun NPC avec l'ID " + npcId + " trouvé dans le fichier JSON.");
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la suppression d'un PNJ du fichier JSON : ", e);
        }
    }

    public static Map<UUID, TradeNpc> loadTradeNpcsFromJson(Level world) {
        Map<UUID, TradeNpc> tradeNpcsMap = new HashMap<>();

        if (world.isClientSide) {
            LOGGER.warn("Tentative de charger des PNJs côté client. Ignoré...");
            return tradeNpcsMap;
        }

        try {
            JsonObject json = readJsonFile(pathNpcs);
            if (!json.has("npcs")) {
                LOGGER.warn("Aucun PNJ trouvé dans le fichier JSON.");
                return tradeNpcsMap;
            }

            JsonArray npcsArray = json.getAsJsonArray("npcs");

            for (JsonElement element : npcsArray) {
                JsonObject npcObject = element.getAsJsonObject();

                String npcId = npcObject.get("id").getAsString();
                String name = npcObject.get("name").getAsString();
                String category = npcObject.get("category").getAsString();
                int x = npcObject.get("x").getAsInt();
                int y = npcObject.get("y").getAsInt();
                int z = npcObject.get("z").getAsInt();

                UUID uuid;
                try {
                    uuid = UUID.fromString(npcId);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("UUID invalide pour le PNJ : " + npcId);
                    continue;
                }

                // Create and store TradeNpc in map
                TradeNpc tradeNpc = new TradeNpc(npcId, name, GlobalNpcManager.getNpcData(name), category, new BlockPos(x, y, z));
                tradeNpcsMap.put(uuid, tradeNpc);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors du chargement des PNJs depuis le fichier JSON : ", e);
        }

        return tradeNpcsMap;  // Return the map of TradeNpc objects
    }

    /////////////////////////////////////////////COMMERCIAL ROAD/////////////////////////////////////////////

    public static void saveRoadToFile(CommercialRoad newRoad) {
        JsonArray roadsArray = new JsonArray();
        Path filePath = Paths.get(pathCommercial);

        // Étape 1 : Charger le fichier existant s’il existe
        if (Files.exists(filePath)) {
            try (Reader reader = Files.newBufferedReader(filePath)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonObject() && root.getAsJsonObject().has("roads")) {
                    roadsArray = root.getAsJsonObject().getAsJsonArray("roads");
                }
            } catch (IOException e) {
                LOGGER.error("Erreur lors de la lecture du fichier commercial_road.json", e);
            }
        }

        // Étape 2 : Convertir la route en JsonObject
        JsonObject newRoadJson = new JsonObject();
        newRoadJson.addProperty("id", newRoad.getId());
        newRoadJson.addProperty("name", newRoad.getName());
        newRoadJson.addProperty("category", newRoad.getCategory());
        newRoadJson.addProperty("minTimer", newRoad.getMinTimer());
        newRoadJson.addProperty("maxTimer", newRoad.getMaxTimer());

        JsonArray positionsArray = new JsonArray();
        for (BlockPos pos : newRoad.getPositions()) {
            JsonObject posObject = new JsonObject();
            posObject.addProperty("x", pos.getX());
            posObject.addProperty("y", pos.getY());
            posObject.addProperty("z", pos.getZ());
            positionsArray.add(posObject);
        }
        newRoadJson.add("positions", positionsArray);

        JsonArray npcEntitiesArray = new JsonArray();
        for (TradeNpcEntity npc : newRoad.getNpcEntities()) {
            JsonObject npcObject = new JsonObject();
            npcObject.addProperty("uuid", npc.getUUID().toString());
            npcEntitiesArray.add(npcObject);
        }
        newRoadJson.add("npcEntities", npcEntitiesArray);

        // Étape 3 : Remplacer la route existante si elle a le même ID
        JsonArray updatedRoads = new JsonArray();
        boolean found = false;
        for (JsonElement element : roadsArray) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("id") && obj.get("id").getAsString().equals(newRoad.getId())) {
                updatedRoads.add(newRoadJson);  // Remplacement
                found = true;
            } else {
                updatedRoads.add(obj);  // Conserver les autres
            }
        }
        if (!found) {
            updatedRoads.add(newRoadJson); // Ajout si non existant
        }

        // Étape 4 : Sauvegarder le fichier
        JsonObject root = new JsonObject();
        root.add("roads", updatedRoads);

        try (Writer writer = Files.newBufferedWriter(filePath)) {
            writer.write(root.toString());
            LOGGER.info("✅ Route commerciale sauvegardée : " + newRoad.getName());
        } catch (IOException e) {
            LOGGER.error("Erreur lors de l'enregistrement de la route commerciale : " + newRoad.getId(), e);
        }
    }

    public static ArrayList<CommercialRoad> loadAllCommercialRoads(ServerLevel world) {
        ArrayList<CommercialRoad> commercialRoads = new ArrayList<>();
        JsonObject jsonObject = readJsonFile(pathCommercial);

        if (jsonObject == null || !jsonObject.has("roads") || !jsonObject.get("roads").isJsonArray()) {
            LOGGER.warn("Aucune route commerciale trouvée dans le fichier JSON.");
            return commercialRoads;
        }

        JsonArray roadsArray = jsonObject.getAsJsonArray("roads");

        for (JsonElement element : roadsArray) {
            JsonObject roadObj = element.getAsJsonObject();
            try {
                String id = roadObj.get("id").getAsString();
                String name = roadObj.get("name").getAsString();
                String category = roadObj.get("category").getAsString();
                int minTimer = roadObj.get("minTimer").getAsInt();
                int maxTimer = roadObj.get("maxTimer").getAsInt();

                ArrayList<BlockPos> positions = new ArrayList<>();
                JsonArray posArray = roadObj.getAsJsonArray("positions");
                for (JsonElement posElem : posArray) {
                    JsonObject posObj = posElem.getAsJsonObject();
                    int x = posObj.get("x").getAsInt();
                    int y = posObj.get("y").getAsInt();
                    int z = posObj.get("z").getAsInt();
                    positions.add(new BlockPos(x, y, z));
                }

                ArrayList<TradeNpcEntity> npcEntities = new ArrayList<>();
                HashMap<BlockPos, Mob> roadMap = new HashMap<>();

                if (roadObj.has("npcEntities")) {
                    JsonArray npcArray = roadObj.getAsJsonArray("npcEntities");
                    for (JsonElement npcElement : npcArray) {
                        JsonObject npcObj = npcElement.getAsJsonObject();
                        if (npcObj.has("uuid")) {
                            UUID npcUuid = UUID.fromString(npcObj.get("uuid").getAsString());
                            Entity entity = world.getEntity(npcUuid);
                            if (entity instanceof TradeNpcEntity tradeNpcEntity) {
                                npcEntities.add(tradeNpcEntity);
                                BlockPos pos = tradeNpcEntity.blockPosition();
                                roadMap.put(pos, tradeNpcEntity);
                                LOGGER.info("🔁 PNJ réassigné à la route : " + tradeNpcEntity.getName().getString());
                            } else {
                                LOGGER.warn("⚠️ Aucun TradeNpcEntity trouvé pour l'UUID : " + npcUuid);
                            }
                        }
                    }
                }

                CommercialRoad road = new CommercialRoad(id, name, category, positions, npcEntities, minTimer, maxTimer);
                commercialRoads.add(road);

                // 🔐 Ajout dans activeNPCs ici
                NpcSpawnerManager.activeNPCs.put(road, roadMap);

            } catch (Exception e) {
                LOGGER.error("❌ Erreur lors du traitement d'une route commerciale.", e);
            }
        }

        LOGGER.info("✅ Chargement de " + commercialRoads.size() + " routes commerciales depuis le fichier JSON.");
        return commercialRoads;
    }
    public static void removeNpcFromRoadJson(UUID npcUuid) {
        JsonObject jsonObject = readJsonFile(pathCommercial);

        if (!jsonObject.has("roads")) return;

        JsonArray roadsArray = jsonObject.getAsJsonArray("roads");

        for (JsonElement roadElement : roadsArray) {
            JsonObject roadObj = roadElement.getAsJsonObject();
            if (!roadObj.has("npcEntities")) continue;

            JsonArray npcArray = roadObj.getAsJsonArray("npcEntities");
            JsonArray updatedNpcArray = new JsonArray();
            boolean removed = false;

            for (JsonElement npcElement : npcArray) {
                JsonObject npcObj = npcElement.getAsJsonObject();
                if (npcObj.has("uuid") && npcObj.get("uuid").getAsString().equals(npcUuid.toString())) {
                    removed = true;
                    continue; // On ignore ce PNJ pour le supprimer
                }
                updatedNpcArray.add(npcObj);
            }

            if (removed) {
                roadObj.add("npcEntities", updatedNpcArray);
                LOGGER.info("🗑️ PNJ supprimé de la route : " + npcUuid);
            }
        }

        writeJsonFile(pathCommercial, jsonObject);
    }

}