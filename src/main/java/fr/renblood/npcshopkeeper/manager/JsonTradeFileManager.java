package fr.renblood.npcshopkeeper.manager;

import com.google.gson.*;
import fr.renblood.npcshopkeeper.data.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
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
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return JsonParser.parseString(content.toString()).getAsJsonObject();
        } catch (FileNotFoundException e) {
            LOGGER.error("Le fichier JSON est introuvable : " + filePath.toString());
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la lecture du fichier JSON : " + e.getMessage());
        } catch (JsonSyntaxException e) {
            LOGGER.error("Le fichier JSON est mal formé : " + e.getMessage());
        }
        return new JsonObject(); // Retourne un objet JSON vide en cas d'erreur
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
    public static void logTradeStart(ServerPlayer player, String tradeName, String id, List<Map<String, Object>> tradeTripleList) {
        Path path = Paths.get(player.getServer().getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade_history.json").toString());
        JsonObject jsonObject = readJsonFile(path);

        JsonArray historyArray = jsonObject.has("history") ? jsonObject.getAsJsonArray("history") : new JsonArray();
        boolean tradeExists = false;

        // Parcourir l'historique pour vérifier s'il y a un trade non terminé pour ce joueur et ce tradeName
        for (JsonElement element : historyArray) {
            JsonObject tradeEntry = element.getAsJsonObject();

            String tradeId = tradeEntry.get("id").getAsString();
            boolean isFinished = tradeEntry.get("isFinished").getAsBoolean();

            if (tradeId.equals(id) && !isFinished) {
                tradeExists = true;
                break;
            }
        }

        // Si aucun trade non terminé trouvé, créer une nouvelle entrée
        if (!tradeExists) {
            JsonObject newTradeEntry = new JsonObject();
            newTradeEntry.addProperty("id", id);
            newTradeEntry.addProperty("player", player.getName().getString());
            newTradeEntry.addProperty("tradeName", tradeName);
            newTradeEntry.addProperty("isFinished", false);

            // Ajouter la date et l'heure du trade
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            newTradeEntry.addProperty("dateTime", formattedDateTime);

            // Créer un tableau JSON pour les trades
            JsonArray tradesArray = new JsonArray();

            // Parcourir la liste des trades (tradeTripleList) et créer un objet JSON pour chaque trade
            for (Map<String, Object> trade : tradeTripleList) {
                JsonObject tradeObject = new JsonObject();
                tradeObject.addProperty("item", (String) trade.get("item")); // Nom de l'item
                tradeObject.addProperty("count", (int) trade.get("quantity")); // Quantité
                tradeObject.addProperty("price", (int) trade.get("price")); // Prix

                tradesArray.add(tradeObject); // Ajouter à la liste des trades
            }

            // Ajouter le tableau des trades à l'entrée du nouveau trade
            newTradeEntry.add("trades", tradesArray);

            // Ajouter cette nouvelle entrée dans l'historique
            historyArray.add(newTradeEntry);
            jsonObject.add("history", historyArray);

            // Sauvegarder le fichier JSON
            writeJsonFile(path, jsonObject);

            LOGGER.info("Trade loggué pour le joueur " + player.getName().getString() + " et le trade " + tradeName);
        } else {
            LOGGER.info("Un trade non terminé existe déjà pour le joueur " + player.getName().getString() + " et le trade " + tradeName);
        }
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

//    public static TradeHistoryList getTradeHistoryList(ServerPlayer player) {
//        TradeHistoryList tradeHistoryList = new TradeHistoryList();
//
//        // Obtenir le chemin du fichier JSON
//        Path path = Paths.get(player.getServer().getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade_history.json").toString());
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
//            StringBuilder content = new StringBuilder();
//            String line;
//
//            // Lire tout le contenu du fichier
//            while ((line = reader.readLine()) != null) {
//                content.append(line);
//            }
//
//            if (content.length() > 0) {
//                JsonObject fileObject = JsonParser.parseString(content.toString()).getAsJsonObject();
//                JsonArray historyArray = fileObject.getAsJsonArray("history");
//
//                // Parcourir chaque entrée de l'historique des trades
//                for (JsonElement tradeElement : historyArray) {
//                    JsonObject tradeObject = tradeElement.getAsJsonObject();
//                    String ID = tradeObject.get("id").getAsString();
//                    String playerName = tradeObject.get("player").getAsString();
//                    String tradeName = tradeObject.get("tradeName").getAsString();
//                    boolean isFinished = tradeObject.get("isFinished").getAsBoolean();
//
//                    // Créer un objet TradeHistory et l'ajouter à la liste
//                    TradeHistory tradeHistory = new TradeHistory(playerName, tradeName, isFinished,, );
//                    tradeHistoryList.addTradeHistory(tradeHistory);
//                }
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return tradeHistoryList; // Retourne la liste des TradeHistory
//    }
    public static TradeHistory getTradeHistoryById(String id) {
        JsonObject jsonObject = readJsonFile(Path.of(pathHistory));

        if (jsonObject.has("history")) {
            JsonArray historyArray = jsonObject.getAsJsonArray("history");
            for (JsonElement tradeElement : historyArray) {
                JsonObject tradeObject = tradeElement.getAsJsonObject();
                String tradeId = tradeObject.get("id").getAsString();

                if (tradeId.equals(id)) {
                    String playerName = tradeObject.get("player").getAsString();
                    String tradeName = tradeObject.get("tradeName").getAsString();
                    boolean isFinished = tradeObject.get("isFinished").getAsBoolean();

                    // Récupération des tradeItems
                    List<Map<String, Object>> tradeItems = new ArrayList<>();
                    if (tradeObject.has("trades")) {
                        JsonArray tradesArray = tradeObject.getAsJsonArray("trades");

                        for (JsonElement tradeItemElement : tradesArray) {
                            JsonObject tradeItemObject = tradeItemElement.getAsJsonObject();
                            Map<String, Object> tradeItemMap = new HashMap<>();

                            String item = tradeItemObject.get("item").getAsString();
                            int count = tradeItemObject.get("count").getAsInt();
                            int price = tradeItemObject.get("price").getAsInt();

                            tradeItemMap.put("item", item);
                            tradeItemMap.put("count", count);
                            tradeItemMap.put("price", price);

                            tradeItems.add(tradeItemMap);
                        }
                    }

                    return new TradeHistory(playerName, tradeName, isFinished, tradeId, tradeItems); // Renvoie le TradeHistory
                }
            }
        }

        LOGGER.warn("Aucun trade avec l'ID " + id + " n'a été trouvé.");
        return null;
    }
}
