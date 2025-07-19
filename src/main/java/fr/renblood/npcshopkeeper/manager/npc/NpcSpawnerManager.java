package fr.renblood.npcshopkeeper.manager.npc;

import com.google.gson.Gson;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
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

    public static void trySpawnNpcForRoad(ServerLevel level, CommercialRoad road) {
        LOGGER.info("🔍 Tentative de spawn pour la route : " + road.getName());

        activeNPCs.computeIfAbsent(road, r -> new HashMap<>());
        HashMap<BlockPos, Mob> roadNPCs = activeNPCs.get(road);

        // Nettoyage des entités invalides
        roadNPCs.entrySet().removeIf(e ->
                !(e.getValue() instanceof TradeNpcEntity) || e.getValue().isRemoved()
        );

        boolean spawned = false;
        for (BlockPos point : road.getPositions()) {
            if (!roadNPCs.containsKey(point)) {
                LOGGER.info("🧭 Point libre trouvé à : " + point);

                String npcName = GlobalNpcManager.getRandomInactiveNpc();
                if (npcName == null) {
                    LOGGER.error("❌ Aucun PNJ inactif disponible.");
                    return;
                }
                Map<String, Object> npcData = GlobalNpcManager.getNpcData(npcName);
                if (npcData == null) {
                    LOGGER.error("❌ Aucune donnée trouvée pour le PNJ : " + npcName);
                    return;
                }

                // Création du modèle et de l'entité
                TradeNpc modelNpc = new TradeNpc(npcName, npcData, road.getCategory(), point);
                TradeNpcEntity npcEntity = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), level);
                modelNpc.setNpcId(npcEntity.getStringUUID());
                npcEntity.setTradeNpc(modelNpc);

                level.addFreshEntity(npcEntity);
                roadNPCs.put(point, npcEntity);
                road.getNpcEntities().add(npcEntity);

                // --- Persistance de la route mise à jour ---
                JsonRepository<CommercialRoad> roadRepo = new JsonRepository<>(
                        Paths.get(JsonFileManager.pathCommercial),
                        "roads",
                        json -> CommercialRoad.fromJson(json, level),
                        CommercialRoad::toJson
                );
                List<CommercialRoad> allRoads = roadRepo.loadAll().stream()
                        .filter(r -> !r.getId().equals(road.getId()))
                        .collect(Collectors.toList());
                allRoads.add(road);
                roadRepo.saveAll(allRoads);

                // --- Persistance du nouveau PNJ ---
                JsonRepository<TradeNpc> npcRepo = new JsonRepository<>(
                        Paths.get(JsonFileManager.pathNpcs),
                        "npcs",
                        TradeNpc::fromJson,
                        TradeNpc::toJson
                );
                npcRepo.add(modelNpc);

                LOGGER.info("✅ PNJ spawné et persistant sur la route '" + road.getName() + "' à " + point + " : " + npcName);
                spawned = true;
                break;
            }
        }

        if (!spawned) {
            LOGGER.error("❌ Tous les points sont occupés : aucun PNJ spawné.");
        }
    }
}
