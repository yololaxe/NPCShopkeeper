// fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager

package fr.renblood.npcshopkeeper.manager.server;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import fr.renblood.npcshopkeeper.manager.road.RoadTickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class OnServerStartedManager {
    private static final Logger LOGGER = LogManager.getLogger(OnServerStartedManager.class);

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

        // ── Initialisation des chemins JSON ────────────────────────────────
        var root = server.getWorldPath(LevelResource.ROOT);
        PATH            = root.resolve("npcshopkeeper/trade.json").toString();
        PATH_HISTORY    = root.resolve("npcshopkeeper/trade_history.json").toString();
        PATH_CONSTANT   = root.resolve("npcshopkeeper/constant.json").toString();
        PATH_PRICE      = root.resolve("npcshopkeeper/price_references.json").toString();
        PATH_COMMERCIAL = root.resolve("npcshopkeeper/commercial_road.json").toString();
        PATH_NPCS       = root.resolve("npcshopkeeper/trades_npcs.json").toString();

        LOGGER.info("Chemins JSON init...");
        checkFileExists(PATH, "trade.json");
        checkFileExists(PATH_HISTORY, "trade_history.json");
        checkFileExists(PATH_CONSTANT, "constant.json");
        checkFileExists(PATH_PRICE, "price_references.json");
        checkFileExists(PATH_COMMERCIAL, "commercial_road.json");
        checkFileExists(PATH_NPCS, "trades_npcs.json");
        // ────────────────────────────────────────────────────────────────────

        ServerLevel world = server.overworld();

        // ── Chargement de TOUS les TradeNpc depuis le JSON ──────────────────
        JsonRepository<TradeNpc> npcRepo = new JsonRepository<>(
                Paths.get(PATH_NPCS),
                "npcs",
                TradeNpc::fromJson,
                TradeNpc::toJson
        );
        List<TradeNpc> allNpcs = npcRepo.loadAll();

        // Séparation PNJ “fixes” vs “de route”
        List<TradeNpc> fixedNpcs = allNpcs.stream()
                .filter(n -> !n.isRouteNpc())
                .collect(Collectors.toList());
        List<TradeNpc> routeNpcs = allNpcs.stream()
                .filter(TradeNpc::isRouteNpc)
                .collect(Collectors.toList());

        // ── Spawn / sync des PNJ fixes ─────────────────────────────────────
        var existingFixed = world.getEntitiesOfClass(TradeNpcEntity.class, fullWorldAABB());
        // suppression
        existingFixed.stream()
                .filter(e -> fixedNpcs.stream()
                        .noneMatch(n -> n.getNpcId().equals(e.getUUID().toString()))
                ).forEach(e -> {
                    LOGGER.warn("❌ PNJ fixe supprimé : {} ({})", e.getUUID(), e.getName().getString());
                    e.discard();
                });
        // ajout
        fixedNpcs.forEach(npcData -> {
            boolean present = existingFixed.stream()
                    .anyMatch(e -> e.getUUID().toString().equals(npcData.getNpcId()));
            if (!present) {
                TradeNpcEntity ent = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
                ent.setUUID(java.util.UUID.fromString(npcData.getNpcId()));
                ent.setTradeNpc(npcData);
                ent.setPos(npcData.getPos().getX(), npcData.getPos().getY(), npcData.getPos().getZ());
                world.addFreshEntity(ent);
                LOGGER.info("📦 PNJ fixe ajouté : {} ({})", npcData.getNpcId(), npcData.getNpcName());
            }
        });
        // ────────────────────────────────────────────────────────────────────

        // Charge des données globales
        GlobalNpcManager.loadNpcData();

        // ── Chargement des routes commerciales ───────────────────────────────
        JsonRepository<CommercialRoad> roadRepo = new JsonRepository<>(
                Paths.get(PATH_COMMERCIAL),
                "roads",
                json -> CommercialRoad.fromJson(json, world),
                CommercialRoad::toJson
        );
        Npcshopkeeper.COMMERCIAL_ROADS = roadRepo.loadAll();

        // Reconstruit activeNPCs **sans** re-spawn
        for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
            var map = new HashMap<BlockPos, Mob>();
            world.getEntitiesOfClass(TradeNpcEntity.class, fullWorldAABB()).stream()
                    .filter(e -> road.getPositions().contains(e.blockPosition()))
                    .forEach(e -> {
                        // on ré-applique le modèle pour remontée skin & trade
                        TradeNpc model = routeNpcs.stream()
                                .filter(n -> n.getNpcId().equals(e.getUUID().toString()))
                                .findFirst().orElse(null);
                        if (model != null) {
                            e.setTradeNpc(model);
                        }
                        map.put(e.blockPosition(), e);
                    });
            NpcSpawnerManager.activeNPCs.put(road, map);
            RoadTickScheduler.registerRoad(road);
        }
        LOGGER.info("✅ activeNPCs prérempli pour {} routes", NpcSpawnerManager.activeNPCs.size());
        // ────────────────────────────────────────────────────────────────────

        LOGGER.info("✅ Initialisation des PNJs et des routes terminée.");
    }

    private static AABB fullWorldAABB() {
        return new AABB(
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
        );
    }

    private static void checkFileExists(String path, String desc) {
        File f = new File(path);
        if (!f.exists()) LOGGER.error("Le fichier {} n'existe pas : {}", desc, path);
        else             LOGGER.info("Le fichier {} existe : {}", desc, path);
    }
}
