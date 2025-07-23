package fr.renblood.npcshopkeeper.manager.npc;

import com.google.gson.Gson;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.Writer;
import java.util.*;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static fr.renblood.npcshopkeeper.Npcshopkeeper.COMMERCIAL_ROADS;

public class NpcSpawnerManager {

    public static final HashMap<CommercialRoad, HashMap<BlockPos, Mob>> activeNPCs = new HashMap<>();
    private static final Random random = new Random();
    private static final Logger LOGGER = LogManager.getLogger(NpcSpawnerManager.class);

    // Appelle cette méthode périodiquement
    public static void updateSpawn(ServerLevel world, CommercialRoad road) {
        if (!activeNPCs.containsKey(road)) {
            activeNPCs.put(road, new HashMap<>());
        }
    }


    public static int getRandomTime(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private static final String NPC_DATA_FILE = "npc_data.json"; // Chemin du fichier JSON
    private static List<NpcData> npcDataList;


    public static class NpcData {
        public String id;
        public String name;
        public String texture;
        public int x, y, z;
        public String category;
    }

    public static void saveNpcData(ServerLevel world) {
        List<NpcData> npcDataList = new ArrayList<>();

        world.getEntities().getAll().forEach(entity -> {
            if (entity instanceof TradeNpcEntity) {
                TradeNpcEntity npc = (TradeNpcEntity) entity;
                NpcData npcData = new NpcData();
                npcData.id = npc.getNpcId();
                npcData.name = npc.getNpcName();
                npcData.texture = npc.getTexture();
                npcData.x = (int) npc.getX();
                npcData.y = (int) npc.getY();
                npcData.z = (int) npc.getZ();
                npcData.category = npc.getTradeCategory();
                npcDataList.add(npcData);
            }
        });

        try (Writer writer = new FileWriter(NPC_DATA_FILE)) {
            Gson gson = new Gson();
            gson.toJson(npcDataList, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trySpawnNpcForRoad(ServerLevel world, CommercialRoad road) {
        // Récupère (ou crée) la map des PNJs actifs pour cette route
        // 0) Si on a déjà un PNJ à chaque position, on ne spawn plus rien
        int occupied = activeNPCs.getOrDefault(road, new HashMap<>()).size();
        int capacity = road.getPositions().size();
        if (occupied >= capacity) {
            LOGGER.info("⚠️ Toutes les positions ({}/{}) sont déjà occupées sur la route '{}', aucun spawn supplémentaire.", occupied, capacity, road.getName());
            return;
        }
            // Récupère (ou crée) la map des PNJs actifs pour cette route

        var roadMap = activeNPCs.computeIfAbsent(road, r -> new HashMap<>());

        // 1) Nettoyage des PNJs invalides
        roadMap.entrySet().removeIf(e ->
                !(e.getValue() instanceof TradeNpcEntity) || e.getValue().isRemoved()
        );

        // 2) On cherche le premier point libre pour spawn
        for (BlockPos pt : road.getPositions()) {
            if (roadMap.containsKey(pt)) continue;

            LOGGER.info("🧭 Spawn NPC sur route '{}' au point {}", road.getName(), pt);

            // ── 2.1) Choix d'un PNJ inactif
            String npcName = GlobalNpcManager.getRandomInactiveNpc();
            if (npcName == null) {
                LOGGER.error("❌ Aucun PNJ inactif disponible pour la route '{}'", road.getName());
                return;
            }
            var npcData = GlobalNpcManager.getNpcData(npcName);

            // ── 2.2) Création du modèle TradeNpc
            TradeNpc modelNpc = new TradeNpc(npcName, npcData, road.getCategory(), pt);
            modelNpc.setRouteNpc(true);

            // ── 2.3) **Chargement et assignation d'un Trade**
            JsonRepository<Trade> tradeRepo = new JsonRepository<>(
                    Paths.get(OnServerStartedManager.PATH),    // chemin vers trades.json
                    "trades",
                    Trade::fromJson,
                    Trade::toJson
            );
            List<Trade> available = tradeRepo.loadAll().stream()
                    .filter(t -> t.getCategory().equalsIgnoreCase(modelNpc.getTradeCategory()))
                    .toList();
            if (!available.isEmpty()) {
                Trade chosen = available.get(random.nextInt(available.size()));
                modelNpc.setTrade(chosen);
                LOGGER.info("✅ Trade '{}' assigné à PNJ '{}'", chosen.getName(), npcName);
            } else {
                LOGGER.warn("⚠️ Aucun trade disponible pour la catégorie '{}'", modelNpc.getTradeCategory());
            }

            // ── 2.4) Création et initialisation de l'entité
            TradeNpcEntity npcEnt = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
            modelNpc.setNpcId(npcEnt.getStringUUID());
            npcEnt.setTradeNpc(modelNpc);
            npcEnt.setPos(pt.getX(), pt.getY(), pt.getZ());
            world.addFreshEntity(npcEnt);

            // … après world.addFreshEntity(npcEnt);
            roadMap.put(pt, npcEnt);
// au lieu de saveAll(COMMERCIAL_ROADS) :
            road.addNpcAndPersist(npcEnt, COMMERCIAL_ROADS);
            LOGGER.info("💾 PNJ '{}' ajouté et route '{}' mise à jour", npcName, road.getName());


            // ── 2.7) Persistance du nouveau PNJ dans trades_npcs.json
            JsonRepository<TradeNpc> npcRepo = new JsonRepository<>(
                    Paths.get(OnServerStartedManager.PATH_NPCS),
                    "npcs",
                    TradeNpc::fromJson,
                    TradeNpc::toJson
            );
            npcRepo.add(modelNpc);
            LOGGER.info("💾 PNJ '{}' enregistré dans '{}'", npcName, OnServerStartedManager.PATH_NPCS);

            // Un seul PNJ spawné à la volée, on stoppe la boucle
            return;
        }

        LOGGER.info("⚠️ Tous les points sont occupés sur la route '{}'", road.getName());
    }






}
