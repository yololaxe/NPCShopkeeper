package fr.renblood.npcshopkeeper.manager;

import com.google.gson.*;
import com.ibm.icu.impl.Pair;
import fr.renblood.npcshopkeeper.data.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonTradeFileManager {

    private static final Logger LOGGER = LogManager.getLogger(JsonTradeFileManager.class);
    public static String pathHistory = "";

    public static String path= "";
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (server != null) {
            Path serverPath = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade.json");
            path = serverPath.toString();
            pathHistory = Paths.get(ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade_history.json").toString()).toString();
            LOGGER.info("Chemin complet du fichier trade.json après démarrage : " + path);

            // Vérifiez que le fichier existe à cet emplacement
            File file = new File(path);
            if (!file.exists()) {
                LOGGER.error("Le fichier JSON spécifié n'existe pas à cet emplacement : " + path);
            } else {
                LOGGER.info("Le fichier JSON existe à l'emplacement : " + path);
            }
        } else {
            LOGGER.error("Le serveur est null dans l'événement onServerStarted");
        }
    }

    // Méthode pour lire un fichier JSON
    public static JsonObject readJsonFile(Path filePath) {
        try {
            // Lire le fichier
            String content = Files.readString(filePath);
            JsonElement jsonElement = JsonParser.parseString(content);

            // Vérifier que l'élément est un objet JSON
            if (!jsonElement.isJsonObject()) {
                LOGGER.error("Le contenu du fichier JSON n'est pas un objet JSON valide : {}", filePath);
                return new JsonObject(); // Retourne un objet vide pour éviter l'erreur
            }

            return jsonElement.getAsJsonObject();
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la lecture du fichier JSON : {}", filePath, e);
            return new JsonObject(); // Retourne un objet vide si le fichier ne peut pas être lu
        } catch (Exception e) {
            LOGGER.error("Erreur inattendue lors de la lecture du fichier JSON : {}", filePath, e);
            return new JsonObject(); // Retourne un objet vide si une autre erreur survient
        }
    }


    // Méthode pour écrire dans un fichier JSON
    private static void writeJsonFile(Path filePath, JsonObject jsonObject) {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(jsonObject.toString());
        } catch (IOException e) {
            LOGGER.error("Erreur lors de l'écriture du fichier JSON : " + e.getMessage());
        }
    }

    // Lire les noms des trades depuis le fichier JSON
    public static List<String> readTradeNames() {

        JsonObject jsonObject = readJsonFile(Path.of(path));

        List<String> tradeNames = new ArrayList<>();
        if (jsonObject.has("trades")) {
            JsonArray tradeArray = jsonObject.getAsJsonArray("trades");
            tradeArray.forEach(tradeElement -> {
                String name = tradeElement.getAsJsonObject().get("Name").getAsString();
                tradeNames.add(name);
            });
            LOGGER.info("Total des trades trouvés : " + tradeNames.size());
        } else {
            LOGGER.warn("Aucun trade trouvé dans le fichier JSON. + path :"+path);
        }
        return tradeNames;
    }

    // Méthode pour vérifier si un trade avec un nom spécifique existe déjà dans le fichier JSON
    public static boolean checkTradeName(String newTradeName) {
        JsonObject jsonObject = readJsonFile(Path.of(path));

        if (jsonObject.has("trades")) {
            JsonArray tradeArray = jsonObject.getAsJsonArray("trades");
            for (JsonElement tradeElement : tradeArray) {
                String existingTradeName = tradeElement.getAsJsonObject().get("Name").getAsString();
                if (existingTradeName.equalsIgnoreCase(newTradeName)) {
                    LOGGER.warn("Un trade avec le nom " + newTradeName + " existe déjà.");
                    return false; // Nom déjà utilisé
                }
            }
        }
        return true; // Nom valide
    }

    // Récupérer la liste des trades depuis le fichier JSON
    public static List<Trade> getTradeList() {
        List<Trade> tradeList = new ArrayList<>();
        JsonObject jsonObject = readJsonFile(Path.of(path));

        if (jsonObject.has("trades")) {
            JsonArray tradeArray = jsonObject.getAsJsonArray("trades");
            tradeArray.forEach(tradeElement -> {
                JsonObject tradeObject = tradeElement.getAsJsonObject();
                String name = tradeObject.get("Name").getAsString();
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
                tradeList.add(new Trade(name, category, tradeItems, tradeResult));
            });
        } else {
            LOGGER.warn("Le fichier JSON est vide.");
        }
        LOGGER.info("Nombre de trades récupérés : " + tradeList.size());
        return tradeList;
    }

    // Récupérer un trade spécifique par son nom
    public static Trade getTradeByName(String tradeName) {
        JsonObject jsonObject = readJsonFile(Path.of(path));

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
    public static void logTradeStart(ServerPlayer player, String tradeName, String id, List<Map<String, Object>> tradeItems, int totalPrice, List<Player> players, String npcId, String npcName) {
        Path path = Paths.get(player.getServer().getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade_history.json").toString());
        JsonObject jsonObject = readJsonFile(path);

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
        writeJsonFile(path, jsonObject);

        if (tradeExists) {
            LOGGER.info("Un trade non terminé avec le NPC " + npcName + " a été mis à jour avec de nouveaux joueurs.");
        } else {
            LOGGER.info("Un nouveau trade loggué pour le joueur " + player.getName().getString() + " et le NPC " + npcName);
        }
    }

    private static boolean containsPlayer(JsonArray playersArray, String playerName) {
        for (JsonElement element : playersArray) {
            if (element.getAsString().equals(playerName)) {
                return true;
            }
        }
        return false;
    }


    // Méthode pour marquer un trade comme terminé
    public static void markTradeAsFinished(ServerPlayer player, String id) {
        Path path = Paths.get(player.getServer().getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade_history.json").toString());
        JsonObject jsonObject = readJsonFile(path);
//        tradeName = tradeName.substring(1, tradeName.length() - 1);

        JsonArray historyArray = jsonObject.getAsJsonArray("history");
        for (JsonElement tradeElement : historyArray) {
            JsonObject tradeObject = tradeElement.getAsJsonObject();
            if (tradeObject.get("id").getAsString().equals(id) &&
                    tradeObject.get("isFinished").getAsString().equals("false")) {
                tradeObject.addProperty("isFinished", true);
                writeJsonFile(path, jsonObject);
                LOGGER.info("Trade marqué comme terminé pour " + player.getName().getString() + " et le trade " + id);
                return;
            }
        }
        LOGGER.warn("Aucune entrée trouvée pour le joueur " + player.getName().getString() + " et le trade " + id);
    }
    public static TradeHistory getTradeHistoryById(String id) {
        JsonObject jsonObject = readJsonFile(Path.of(pathHistory));

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

//
//    public static Pair<Boolean, TradeHistory> checkTradeStatusForPlayer(ServerPlayer player, String tradeName) {
//        JsonObject jsonObject = readJsonFile(Path.of(pathHistory));
//
//        if (jsonObject.has("history")) {
//            JsonArray historyArray = jsonObject.getAsJsonArray("history");
//
//            // Parcourir l'historique des trades pour ce joueur
//            for (JsonElement tradeElement : historyArray) {
//                JsonObject tradeObject = tradeElement.getAsJsonObject();
//
//                // Vérifier si le trade appartient à ce joueur
//                JsonArray playersArray = tradeObject.getAsJsonArray("players");
//                boolean isPlayerInTrade = false;
//
//                for (JsonElement playerElement : playersArray) {
//                    String playerName = playerElement.getAsString();
//                    if (playerName.equals(player.getName().getString())) {
//                        isPlayerInTrade = true;
//                        break;
//                    }
//                }
//                if (!isPlayerInTrade) {
//                    continue;
//                }
//
//                String tradeId = tradeObject.get("id").getAsString();
//                String npcId = tradeObject.get("npcId").getAsString();
//                String npcName = tradeObject.get("npcName").getAsString();
//                String currentTradeName = tradeObject.get("tradeName").getAsString();
//
//                // Vérifier si le nom du trade correspond à celui recherché
//                if (currentTradeName.equalsIgnoreCase(tradeName)) {
//                    boolean isFinished = tradeObject.get("isFinished").getAsBoolean();
//
//                    // Récupérer les tradeItems
//                    List<Map<String, Object>> tradeItems = new ArrayList<>();
//                    if (tradeObject.has("trades")) {
//                        JsonArray tradesArray = tradeObject.getAsJsonArray("trades");
//
//                        for (JsonElement tradeItemElement : tradesArray) {
//                            JsonObject tradeItemObject = tradeItemElement.getAsJsonObject();
//                            Map<String, Object> tradeItemMap = new HashMap<>();
//
//                            String item = tradeItemObject.get("item").getAsString();
//                            int quantity = tradeItemObject.get("quantity").getAsInt();
//                            int price = tradeItemObject.get("price").getAsInt();
//
//                            tradeItemMap.put("item", item);
//                            tradeItemMap.put("quantity", quantity);
//                            tradeItemMap.put("price", price);
//
//                            tradeItems.add(tradeItemMap);
//                        }
//                    }
//
//                    // Si le trade n'est pas terminé, renvoyer true et le TradeHistory complet
//                    if (!isFinished) {
//                        List<String> playersList = new ArrayList<>();
//                        for (JsonElement playerElement : playersArray) {
//                            playersList.add(playerElement.getAsString());
//                        }
//                        int totalPrice = tradeObject.has("totalPrice") ? tradeObject.get("totalPrice").getAsInt() : 0;
//                        LOGGER.info("Le joueur " + player.getName().getString() + " a un trade non terminé pour " + tradeName);
//                        return Pair.of(true, new TradeHistory(playersList, tradeName, isFinished, tradeId, tradeItems, totalPrice, npcId, npcName));
//                    }
//
//                    // Si le trade est terminé, on passe à la recherche d'un autre trade du même nom
//                }
//            }
//        }
//
//        LOGGER.warn("Aucun trade non terminé trouvé pour le joueur " + player.getName().getString() + " et le trade " + tradeName);
//        return Pair.of(false, new TradeHistory(new ArrayList<>(), "", true, "", new ArrayList<>(), 0, "", "")); // Si aucun trade non terminé trouvé, renvoyer false et un trade vide
//    }



    public static List<String> readCategoryNames() {
        List<String> categories = new ArrayList<>();
        JsonObject jsonObject = readJsonFile(Path.of(path));

        if (jsonObject.has("trades")) {
            JsonArray tradeArray = jsonObject.getAsJsonArray("trades");
            tradeArray.forEach(tradeElement -> {
                String category = tradeElement.getAsJsonObject().get("Category").getAsString();
                if (!categories.contains(category)) {
                    categories.add(category);
                }
            });
        }
        return categories;
    }

    public static List<Trade> getTradesByCategory(String category) {
        List<Trade> trades = new ArrayList<>();
        JsonObject jsonObject = readJsonFile(Path.of(path));

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
    public static Pair<Boolean, TradeHistory> checkTradeStatusForNpc(String npcId) {
        LOGGER.info("Début de la vérification du statut du trade pour le NPC : " + npcId);
        JsonObject jsonObject = readJsonFile(Path.of(pathHistory));

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






}
