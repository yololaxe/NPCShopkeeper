package fr.renblood.npcshopkeeper.manager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.logging.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class OnServerStartedManager {
    private static final Logger LOGGER = LogManager.getLogger(JsonTradeFileManager.class);
    public static String PATH= "";
    public static String PATH_HISTORY = "";
    public static String PATH_CONSTANT= "";
    public static String PATH_PRICE= "";
    public static String PATH_COMMERCIAL= "";

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();

        if (server != null) {
            PATH = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade.json").toString();
            PATH_HISTORY = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade_history.json").toString();
            PATH_CONSTANT = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/constant.json").toString();
            PATH_PRICE = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/price_references.json").toString();
            PATH_COMMERCIAL = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/commercial_road.json").toString();

            LOGGER.info("Chemin complet du fichier trade.json après démarrage : {}", PATH);
            LOGGER.info("Chemin complet du fichier trade_history.json après démarrage : {}", PATH_HISTORY);
            LOGGER.info("Chemin complet du fichier constant.json après démarrage : {}", PATH_CONSTANT);
            LOGGER.info("Chemin complet du fichier price_references.json après démarrage : {}", PATH_PRICE);
            LOGGER.info("Chemin complet du fichier price_references.json après démarrage : {}", PATH_COMMERCIAL);

            checkFileExists(PATH, "trade.json");
            checkFileExists(PATH_HISTORY, "trade_history.json");
            checkFileExists(PATH_CONSTANT, "constant.json");
            checkFileExists(PATH_PRICE, "price_references.json");
            checkFileExists(PATH_COMMERCIAL, "price_references.json");

            GlobalNpcManager.loadNpcData();
            LOGGER.info("PNJs initialisés avec succès.");

        } else {
            LOGGER.error("Le serveur est null dans l'événement onServerStarted");
        }
    }
    private static void checkFileExists(String path, String description) {
        File file = new File(path);
        if (!file.exists()) {
            LOGGER.error("Le fichier {} n'existe pas à l'emplacement : {}", description, path);
        } else {
            LOGGER.info("Le fichier {} existe à l'emplacement : {}", description, path);
        }
    }


}