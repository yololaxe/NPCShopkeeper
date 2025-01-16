package fr.renblood.npcshopkeeper.manager;

import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.logging.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static com.mojang.text2speech.Narrator.LOGGER;

public class OnServerStartedManager {
    private static final Logger LOGGER = LogManager.getLogger(JsonTradeFileManager.class);
    public static String PATH= "";
    public static String PATH_HISTORY = "";
    public static String PATH_CONSTANT= "";
    public static String PATH_PRICE= "";
    public static String PATH_COMMERCIAL= "";
    public static String PATH_NPCS= "";

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();

        if (server != null) {
            PATH = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade.json").toString();
            PATH_HISTORY = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade_history.json").toString();
            PATH_CONSTANT = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/constant.json").toString();
            PATH_PRICE = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/price_references.json").toString();
            PATH_COMMERCIAL = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/commercial_road.json").toString();
            PATH_NPCS = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trades_npcs.json").toString();

            LOGGER.info("Chemin complet du fichier trade.json après démarrage : {}", PATH);
            LOGGER.info("Chemin complet du fichier trade_history.json après démarrage : {}", PATH_HISTORY);
            LOGGER.info("Chemin complet du fichier constant.json après démarrage : {}", PATH_CONSTANT);
            LOGGER.info("Chemin complet du fichier price_references.json après démarrage : {}", PATH_PRICE);
            LOGGER.info("Chemin complet du fichier price_references.json après démarrage : {}", PATH_COMMERCIAL);
            LOGGER.info("Chemin complet du fichier trades_npcs.json après démarrage : {}", PATH_NPCS);

            checkFileExists(PATH, "trade.json");
            checkFileExists(PATH_HISTORY, "trade_history.json");
            checkFileExists(PATH_CONSTANT, "constant.json");
            checkFileExists(PATH_PRICE, "price_references.json");
            checkFileExists(PATH_COMMERCIAL, "price_references.json");
            checkFileExists(PATH_NPCS, "trades_npcs.json");

            ServerLevel world = event.getServer().overworld();
            Map<UUID, TradeNpc> tradeNpcsMap = JsonTradeFileManager.loadTradeNpcsFromJson(world);

            if (!tradeNpcsMap.isEmpty()) {
                for (Map.Entry<UUID, TradeNpc> entry : tradeNpcsMap.entrySet()) {
                    TradeNpc tradeNpc = entry.getValue();
                    BlockPos pos = tradeNpc.getPos();

                    TradeNpcEntity npcEntity = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
                    npcEntity.setTradeNpc(tradeNpc);
                    npcEntity.setPos(pos.getX(), pos.getY(), pos.getZ());

                    world.addFreshEntity(npcEntity);
                }

                LOGGER.info("Tous les PNJs ont été chargés avec succès !");
            } else {
                LOGGER.warn("Aucun PNJ trouvé dans le fichier JSON au chargement du serveur.");
            }

            GlobalNpcManager.loadNpcData();
            //JsonTradeFileManager.loadTradeNpcsFromJson(event.getServer().overworld());
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