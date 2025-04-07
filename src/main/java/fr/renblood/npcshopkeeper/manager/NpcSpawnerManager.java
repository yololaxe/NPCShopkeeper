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

        HashMap<BlockPos, Mob> roadNPCs = activeNPCs.get(road);
        List<BlockPos> positions = road.getPositions();

        for (BlockPos point : positions) {
            if (!roadNPCs.containsKey(point)) { // Si le point est libre
                int timer = getRandomTime(road.getMinTimer(), road.getMaxTimer());

                // Planifier une tâche différée via un "Tick Task"
                world.getServer().execute(() -> {
                    if (!roadNPCs.containsKey(point)) { // Vérifier encore si le point est libre
////                        Mob npc = spawnRandomNPC(world, point, road.getCategory());
//                        if (npc != null) {
//                            roadNPCs.put(point, npc);
//                        }
                    }
                });
            }
        }
    }


//    public static void npcFinished(ServerLevel world, CommercialRoad road, Mob npc) {
//        HashMap<BlockPos, Mob> roadNPCs = activeNPCs.get(road);
//        if (roadNPCs != null) {
//            BlockPos npcPos = roadNPCs.entrySet().stream()
//                    .filter(entry -> entry.getValue() == npc)
//                    .map(HashMap.Entry::getKey)
//                    .findFirst()
//                    .orElse(null);
//
//            if (npcPos != null) {
//                roadNPCs.remove(npcPos); // Libérer le point
//                npc.discard(); // Supprimer le NPC
//            }
//        }
//    }


//    private static Mob spawnRandomNPC(ServerLevel world, BlockPos pos, String category) {
//        // Remplace "EntityType.VILLAGER" par une logique pour sélectionner un type d'entité basé sur la catégorie
//        EntityType<?> entityType = selectRandomEntityTypeByCategory(category);
//        Mob npc = (Mob) entityType.create(world);
//        if (npc != null) {
//            npc.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
//            world.addFreshEntity(npc);
//        }
//        return npc;
//    }
//
//    private static EntityType<?> selectRandomEntityTypeByCategory(String category) {
//        // Remplace par une logique qui retourne un type d'entité basé sur la catégorie
//        return EntityType.VILLAGER; // Exemple par défaut
//    }

    static int getRandomTime(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private static final String NPC_DATA_FILE = "npc_data.json"; // Chemin du fichier JSON
    private static List<NpcData> npcDataList;

    public static void loadNpcData() {
        try (Reader reader = new FileReader(NPC_DATA_FILE)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<NpcData>>() {
            }.getType();
            npcDataList = gson.fromJson(reader, listType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<NpcData> getNpcDataList() {
        return npcDataList;
    }

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

        // 🧹 NE GARDER que les TradeNpcEntity valides
        roadNPCs.entrySet().removeIf(entry ->
                !(entry.getValue() instanceof TradeNpcEntity) || entry.getValue().isRemoved());

        LOGGER.info("➡️ Tentative de spawn pour la route : " + road.getName());
        LOGGER.info("Positions : " + road.getPositions());
        LOGGER.info("Positions occupées (roadNPCs.keySet()) : " + roadNPCs.keySet());
        LOGGER.info("NPCs actifs (roadNPCs.values()) : " + roadNPCs.values());

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

                TradeNpc modelNpc = new TradeNpc(npcName, npcData, road.getCategory(), point);
                TradeNpcEntity npcEntity = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), level);
                modelNpc.setNpcId(npcEntity.getStringUUID());

                npcEntity.setTradeNpc(modelNpc);
                level.addFreshEntity(npcEntity);

                roadNPCs.put(point, npcEntity);
                road.getNpcEntities().add(npcEntity);
                JsonTradeFileManager.saveRoadToFile(road);
                JsonTradeFileManager.addTradeNpcToJson(modelNpc);
                ActiveNpcManager.addActiveNpc(modelNpc);

                LOGGER.info("✅ PNJ spawné sur la route '" + road.getName() + "' à " + point + " : " + npcName);
                break;
            } else {
                LOGGER.error("❌ PATATE");
            }
        }
    }


    public static void startSpawningForRoad(ServerLevel level, CommercialRoad road) {
        if (road == null || level == null) return;

        int delay = getRandomTime(road.getMinTimer(), road.getMaxTimer()) * 20; // ticks
        trySpawnNpcForRoad(level, road); // 👈 spawn immédiat
        NpcSpawnScheduler.scheduleSpawn(level, road, delay);
    }


}
