package fr.renblood.npcshopkeeper.manager.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcSpawnManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import fr.renblood.npcshopkeeper.manager.road.RoadTickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class OnServerStartedManager {
    public static final Logger LOGGER = LogManager.getLogger(OnServerStartedManager.class);

    public static String PATH;
    public static String PATH_HISTORY;
    public static String PATH_CONSTANT;
    public static String PATH_PRICE;
    public static String PATH_XP; // Nouveau chemin
    public static String PATH_COMMERCIAL;
    public static String PATH_NPCS;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            LOGGER.error("Le serveur est null dans onServerStarting");
            return;
        }

        initPaths(server);
        PortManager.init(server); // Initialisation du PortManager

        ServerLevel world = server.overworld();

        // Charge des données globales (liste des noms de PNJ possibles)
        GlobalNpcManager.loadNpcData();

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
        List<TradeNpc> routeModels = allNpcs.stream()
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
                ent.setUUID(UUID.fromString(npcData.getNpcId()));
                ent.setTradeNpc(npcData);
                BlockPos p = npcData.getPos();
                ent.setPos(p.getX(), p.getY(), p.getZ());
                world.addFreshEntity(ent);
                LOGGER.info("📦 PNJ fixe ajouté : {} ({})", npcData.getNpcId(), npcData.getNpcName());
            }
            // Marquer comme actif
            GlobalNpcManager.activateNpc(npcData);
        });
        // ────────────────────────────────────────────────────────────────────

        // ── Chargement des routes commerciales ───────────────────────────────
        JsonRepository<CommercialRoad> roadRepo = new JsonRepository<>(
                Paths.get(PATH_COMMERCIAL),
                "roads",
                json -> CommercialRoad.fromJson(json, world),
                CommercialRoad::toJson
        );
        Npcshopkeeper.COMMERCIAL_ROADS = roadRepo.loadAll();

        // ── Reconstruction des PNJs de route + activeNPCs ───────────────────
        try {
            Path commercialPath = Paths.get(PATH_COMMERCIAL);
            try (Reader reader = Files.newBufferedReader(commercialPath)) {
                JsonObject rootJson = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray roadsArr = rootJson.getAsJsonArray("roads");

                for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
                    Map<BlockPos, Mob> map = new HashMap<>();

                    // retrouve l'objet JSON correspondant à cette route
                    for (JsonElement el : roadsArr) {
                        JsonObject rJson = el.getAsJsonObject();
                        if (!road.getId().equals(rJson.get("id").getAsString())) continue;

                        JsonArray npcArr = rJson.getAsJsonArray("npcEntities");
                        for (JsonElement ne : npcArr) {
                            String uuidStr = ne.getAsJsonObject().get("uuid").getAsString();

                            // retrouve le modèle TradeNpc par UUID
                            TradeNpc model = routeModels.stream()
                                    .filter(m -> m.getNpcId().equals(uuidStr))
                                    .findFirst().orElse(null);
                            if (model == null) {
                                LOGGER.warn("❓ Pas de modèle TradeNpc pour UUID {} sur la route '{}'", uuidStr, road.getName());
                                continue;
                            }

                            // Vérifier si l'entité existe déjà dans le monde pour éviter les doublons
                            List<TradeNpcEntity> existingEntities = world.getEntitiesOfClass(TradeNpcEntity.class, fullWorldAABB());
                            TradeNpcEntity ent = existingEntities.stream()
                                    .filter(e -> e.getUUID().toString().equals(uuidStr))
                                    .findFirst()
                                    .orElse(null);

                            if (ent == null) {
                                // recrée l'entité route-PNJ si elle n'existe pas
                                ent = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
                                ent.setUUID(UUID.fromString(uuidStr));
                                ent.setTradeNpc(model);
                                BlockPos pos = model.getPos();
                                ent.setPos(pos.getX(), pos.getY(), pos.getZ());
                                world.addFreshEntity(ent);
                                LOGGER.info("🔄 PNJ de route recréé : {} ({})", uuidStr, model.getNpcName());
                            } else {
                                LOGGER.info("🔄 PNJ de route déjà présent : {} ({})", uuidStr, model.getNpcName());
                                // S'assurer que le modèle est à jour
                                ent.setTradeNpc(model);
                            }

                            map.put(model.getPos(), ent);
                            road.getNpcEntities().add(ent);

                            // Marquer comme actif
                            GlobalNpcManager.activateNpc(model);
                        }
                        break;
                    }

                    NpcSpawnerManager.activeNPCs.put(road, (HashMap<BlockPos, Mob>) map);
                    RoadTickScheduler.registerRoad(road);
                    LOGGER.info("🔄 Route '{}' initialisée avec {} PNJs de route", road.getName(), map.size());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la reconstruction des PNJs de route", e);
        }
        LOGGER.info("✅ activeNPCs prérempli pour {} routes", NpcSpawnerManager.activeNPCs.size());
        // ────────────────────────────────────────────────────────────────────

        GlobalNpcSpawnManager.synchronize(server);
        LOGGER.info("✅ Initialisation des PNJs et des routes terminée.");
    }

    public static void initPaths(MinecraftServer server) {
        // ── Initialisation des chemins JSON ────────────────────────────────
        var root = server.getWorldPath(LevelResource.ROOT);
        PATH            = root.resolve("npcshopkeeper/trade.json").toString();
        PATH_HISTORY    = root.resolve("npcshopkeeper/trade_history.json").toString();
        PATH_CONSTANT   = root.resolve("npcshopkeeper/constant.json").toString();
        PATH_PRICE      = root.resolve("npcshopkeeper/price_references.json").toString();
        PATH_XP         = root.resolve("npcshopkeeper/xp_references.json").toString(); // Init
        PATH_COMMERCIAL = root.resolve("npcshopkeeper/commercial_road.json").toString();
        PATH_NPCS       = root.resolve("npcshopkeeper/trades_npcs.json").toString();

        LOGGER.info("Chemins JSON init...");
        checkFileExists(PATH, "trade.json");
        checkFileExists(PATH_HISTORY, "trade_history.json");
        checkFileExists(PATH_CONSTANT, "constant.json");
        checkFileExists(PATH_PRICE, "price_references.json");
        checkFileExists(PATH_XP, "xp_references.json"); // Check
        checkFileExists(PATH_COMMERCIAL, "commercial_road.json");
        checkFileExists(PATH_NPCS, "trades_npcs.json");
        // ────────────────────────────────────────────────────────────────────
    }

    private static AABB fullWorldAABB() {
        return new AABB(
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
        );
    }

    private static void checkFileExists(String path, String desc) {
        File f = new File(path);
        if (!f.exists()) {
            // Création du fichier vide si nécessaire pour éviter les erreurs de lecture
            if (desc.equals("xp_references.json")) {
                try {
                    f.getParentFile().mkdirs();
                    try (java.io.FileWriter w = new java.io.FileWriter(f)) {
                        w.write("{\"references\": []}");
                    }
                    LOGGER.info("Fichier {} créé.", desc);
                } catch (Exception e) {
                    LOGGER.error("Impossible de créer {}", desc, e);
                }
            } else {
                LOGGER.error("Le fichier {} n'existe pas : {}", desc, path);
            }
        }
        else             LOGGER.info("Le fichier {} existe : {}", desc, path);
    }
}
