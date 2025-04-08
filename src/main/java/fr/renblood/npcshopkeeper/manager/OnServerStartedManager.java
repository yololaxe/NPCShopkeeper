package fr.renblood.npcshopkeeper.manager;

import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

public class OnServerStartedManager {
    private static final Logger LOGGER = LogManager.getLogger(JsonTradeFileManager.class);
    public static String PATH = "";
    public static String PATH_HISTORY = "";
    public static String PATH_CONSTANT = "";
    public static String PATH_PRICE = "";
    public static String PATH_COMMERCIAL = "";
    public static String PATH_NPCS = "";

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

            LOGGER.info("Chemins JSON init...");
            checkFileExists(PATH, "trade.json");
            checkFileExists(PATH_HISTORY, "trade_history.json");
            checkFileExists(PATH_CONSTANT, "constant.json");
            checkFileExists(PATH_PRICE, "price_references.json");
            checkFileExists(PATH_COMMERCIAL, "commercial_road.json");
            checkFileExists(PATH_NPCS, "trades_npcs.json");

            ServerLevel world = server.overworld();

            // --- CHARGEMENT DES PNJs DEPUIS LE FICHIER JSON ---
            Map<UUID, TradeNpc> tradeNpcsMap = JsonTradeFileManager.loadTradeNpcsFromJson(world);
            Set<UUID> expectedUUIDs = tradeNpcsMap.keySet();

            // --- SUPPRESSION DES PNJs NON ENREGISTRÉS ---
            List<TradeNpcEntity> existingEntities = world.getEntitiesOfClass(TradeNpcEntity.class, new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
            for (TradeNpcEntity entity : existingEntities) {
                UUID uuid = entity.getUUID();
                if (!expectedUUIDs.contains(uuid)) {
                    LOGGER.warn("❌ PNJ inconnu supprimé : " + uuid + " (" + entity.getName().getString() + ")");
                    entity.discard();
                } else {
                    LOGGER.info("✅ PNJ déjà présent : " + uuid + " (" + entity.getName().getString() + ")");
                }
            }

            // --- AJOUT DES PNJs MANQUANTS ---
            for (Map.Entry<UUID, TradeNpc> entry : tradeNpcsMap.entrySet()) {
                UUID uuid = entry.getKey();
                TradeNpc tradeNpc = entry.getValue();

                boolean alreadyPresent = world.getEntitiesOfClass(
                                TradeNpcEntity.class,
                                new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                                        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY))
                        .stream().anyMatch(e -> e.getUUID().equals(uuid));


                if (!alreadyPresent) {
                    TradeNpcEntity npcEntity = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
                    npcEntity.setUUID(uuid);
                    npcEntity.setTradeNpc(tradeNpc);
                    npcEntity.setPos(tradeNpc.getPos().getX(), tradeNpc.getPos().getY(), tradeNpc.getPos().getZ());
                    world.addFreshEntity(npcEntity);
                    LOGGER.info("📦 PNJ ajouté au monde : " + uuid + " (" + tradeNpc.getNpcName() + ")");
                }
            }

            GlobalNpcManager.loadNpcData();
            LOGGER.info("✅ Initialisation des PNJs terminée.");
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
