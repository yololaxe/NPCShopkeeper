package fr.renblood.npcshopkeeper.manager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.*;

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


    static int getRandomTime(int min, int max) {
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

        if (!activeNPCs.containsKey(road)) {
            LOGGER.warn("❗ Route non enregistrée dans activeNPCs, ajout...");
            activeNPCs.put(road, new HashMap<>());
        }

        HashMap<BlockPos, Mob> roadNPCs = activeNPCs.get(road);

        // 🧹 Nettoyer les entités invalides
        roadNPCs.entrySet().removeIf(entry ->
                !(entry.getValue() instanceof TradeNpcEntity) || entry.getValue().isRemoved());

        LOGGER.info("➡️ Tentative de spawn pour la route : {}", road.getName());
        LOGGER.info("Positions : " + road.getPositions());
        LOGGER.info("Positions occupées (roadNPCs.keySet()) : " + roadNPCs.keySet());
        LOGGER.info("NPCs actifs (roadNPCs.values()) : " + roadNPCs.values());

        boolean spawned = false;
        for (BlockPos point : road.getPositions()) {
            if (!roadNPCs.containsKey(point)) {
                LOGGER.info("🧭 Point libre trouvé à : " + point);

//                // 🔒 Vérifie s'il y a déjà une entité présente à cette position
//                List<TradeNpcEntity> alreadyPresent = level.getEntitiesOfClass(
//                        TradeNpcEntity.class,
//                        new AABB(point)
//                );
//
//                if (!alreadyPresent.isEmpty()) {
//                    LOGGER.warn("⚠️ Entité déjà présente à cette position, annulation du spawn.");
//                    continue;
//                }

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

                TradeNpc modelNpc = new TradeNpc(npcName, npcData, road.getCategory(), point);

                TradeNpcEntity npcEntity = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), level);
                modelNpc.setNpcId(npcEntity.getStringUUID());
                npcEntity.setTradeNpc(modelNpc);

                // ✅ Vérifie si l'entité est déjà présente dans roadNPCs (évite les doublons)
                if (roadNPCs.containsValue(npcEntity)) {
                    LOGGER.warn("⚠️ Entité déjà référencée dans roadNPCs, annulation du spawn.");
                    continue;
                }

                try {
                    level.addFreshEntity(npcEntity);
                    LOGGER.info("✅ Entité ajoutée avec succès !");
                } catch (Exception e) {
                    LOGGER.error("❌ Échec lors de l'ajout de l'entité : " + e.getMessage());
                    e.printStackTrace();
                }

                roadNPCs.put(point, npcEntity);
                road.getNpcEntities().add(npcEntity);
                JsonTradeFileManager.saveRoadToFile(road);
                JsonTradeFileManager.addTradeNpcToJson(modelNpc);
//                ActiveNpcManager.addActiveNpc(modelNpc);

                LOGGER.info("✅ PNJ spawné sur la route '" + road.getName() + "' à " + point + " : " + npcName);
                spawned = true;
                break;
            }
        }

        if (!spawned) {
            LOGGER.error("❌ Tous les points sont occupés : aucun PNJ spawné.");
        }
    }




    public static void startSpawningForRoad(ServerLevel level, CommercialRoad road) {
        if (road == null || level == null) return;

        trySpawnNpcForRoad(level, road); // ✅ Spawner au moment de la création
        RoadTickScheduler.registerRoad(road); // ✅ Et laisser le vrai scheduler gérer le reste
    }



}
