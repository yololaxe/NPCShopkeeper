package fr.renblood.npcshopkeeper.manager;

import com.google.gson.JsonArray;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

import static fr.renblood.npcshopkeeper.manager.JsonTradeFileManager.readJsonFile;


public class ConstantManager{
    private static final Logger LOGGER = LogManager.getLogger(JsonTradeFileManager.class);
    public static String path = "";
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (server != null) {
            Path serverPath = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/constant.json");
            path = serverPath.toString();
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
    public static JsonArray getPnjData() {
        return readJsonFile(Path.of(path)).getAsJsonArray();
    }
}
