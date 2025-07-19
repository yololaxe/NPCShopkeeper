package fr.renblood.npcshopkeeper.manager.server;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager; // pour les chemins statiques
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
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
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class OnServerStartedManager {
    public static final Logger LOGGER = LogManager.getLogger(OnServerStartedManager.class);
    public static String PATH;
    public static String PATH_HISTORY;
    public static String PATH_CONSTANT;
    public static String PATH_PRICE;
    public static String PATH_COMMERCIAL;
    public static String PATH_NPCS;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            LOGGER.error("Le serveur est null dans onServerStarted");
            return;
        }

        // Initialisation des chemins
        var root = server.getWorldPath(LevelResource.ROOT);
        PATH         = root.resolve("npcshopkeeper/trade.json").toString();
        PATH_HISTORY = root.resolve("npcshopkeeper/trade_history.json").toString();
        PATH_CONSTANT= root.resolve("npcshopkeeper/constant.json").toString();
        PATH_PRICE   = root.resolve("npcshopkeeper/price_references.json").toString();
        PATH_COMMERCIAL= root.resolve("npcshopkeeper/commercial_road.json").toString();
        PATH_NPCS    = root.resolve("npcshopkeeper/trades_npcs.json").toString();

        LOGGER.info("Chemins JSON init...");
        checkFileExists(PATH, "trade.json");
        checkFileExists(PATH_HISTORY, "trade_history.json");
        checkFileExists(PATH_CONSTANT, "constant.json");
        checkFileExists(PATH_PRICE, "price_references.json");
        checkFileExists(PATH_COMMERCIAL, "commercial_road.json");
        checkFileExists(PATH_NPCS, "trades_npcs.json");

        ServerLevel world = server.overworld();

        // ── CHARGEMENT DES TradeNpc via JsonRepository ──
        JsonRepository<TradeNpc> npcRepo = new JsonRepository<>(
                Paths.get(PATH_NPCS),
                "npcs",
                TradeNpc::fromJson,
                TradeNpc::toJson
        );
        List<TradeNpc> npcList = npcRepo.loadAll();
        Map<UUID, TradeNpc> tradeNpcsMap = npcList.stream()
                .collect(Collectors.toMap(
                        npc -> UUID.fromString(npc.getNpcId()),
                        npc -> npc
                ));
        Set<UUID> expectedUUIDs = tradeNpcsMap.keySet();

        // ── SUPPRESSION DES ENTITÉS NON ENREGISTRÉES ──
        List<TradeNpcEntity> existing = world.getEntitiesOfClass(
                TradeNpcEntity.class,
                new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        );
        for (TradeNpcEntity ent : existing) {
            UUID uuid = ent.getUUID();
            if (!expectedUUIDs.contains(uuid)) {
                LOGGER.warn("❌ PNJ inconnu supprimé : {} ({})", uuid, ent.getName().getString());
                ent.discard();
            } else {
                LOGGER.info("✅ PNJ déjà présent : {} ({})", uuid, ent.getName().getString());
            }
        }

        // ── AJOUT DES PNJs MANQUANTS ──
        for (Map.Entry<UUID, TradeNpc> entry : tradeNpcsMap.entrySet()) {
            UUID uuid = entry.getKey();
            TradeNpc data = entry.getValue();
            boolean already = existing.stream().anyMatch(e -> e.getUUID().equals(uuid));
            if (!already) {
                TradeNpcEntity ent = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
                ent.setUUID(uuid);
                ent.setTradeNpc(data);
                ent.setPos(data.getPos().getX(), data.getPos().getY(), data.getPos().getZ());
                world.addFreshEntity(ent);
                LOGGER.info("📦 PNJ ajouté au monde : {} ({})", uuid, data.getNpcName());
            }
        }

        // Charge les PNJs statiques
        GlobalNpcManager.loadNpcData();

        // ── CHARGEMENT DES CommercialRoad via JsonRepository ──
        JsonRepository<CommercialRoad> roadRepo = new JsonRepository<>(
                Paths.get(PATH_COMMERCIAL),
                "roads",
                json -> CommercialRoad.fromJson(json, world),
                CommercialRoad::toJson
        );
        // after
        Npcshopkeeper.COMMERCIAL_ROADS = new ArrayList<>(roadRepo.loadAll());

        LOGGER.info("✅ Initialisation des PNJs et routes terminée.");
    }

    private static void checkFileExists(String path, String desc) {
        File f = new File(path);
        if (!f.exists()) {
            LOGGER.error("Le fichier {} n'existe pas à l'emplacement : {}", desc, path);
        } else {
            LOGGER.info("Le fichier {} existe : {}", desc, path);
        }
    }
}
