package fr.renblood.npcshopkeeper.manager.npc;

import com.google.gson.Gson;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
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
        // 1) Récupère (ou crée) la map des PNJ actifs pour cette route
        var roadMap = activeNPCs.computeIfAbsent(road, r -> new HashMap<>());

        // 2) Nettoie les entrées invalides (PNJ supprimés ou non-TradeNpcEntity)
        roadMap.entrySet().removeIf(e ->
                !(e.getValue() instanceof TradeNpcEntity) || e.getValue().isRemoved()
        );

        boolean anySpawned = false;

        // 3) Pour chaque point libre de la route, on spawn un nouveau PNJ
        for (BlockPos pt : road.getPositions()) {
            if (roadMap.containsKey(pt)) {
                continue;
            }

            LOGGER.info("🧭 Spawn NPC sur route '{}' au point {}", road.getName(), pt);

            // 3.a) Récupère un PNJ inactif
            String npcName = GlobalNpcManager.getRandomInactiveNpc();
            if (npcName == null) {
                LOGGER.error("❌ Aucun PNJ inactif disponible pour spawn.");
                break;
            }
            Map<String, Object> npcData = GlobalNpcManager.getNpcData(npcName);

            // 3.b) Construit le modèle TradeNpc et marque-le comme 'routeNpc'
            TradeNpc modelNpc = new TradeNpc(npcName, npcData, road.getCategory(), pt);
            modelNpc.setRouteNpc(true);

            // 3.c) Crée et initialise l'entité
            TradeNpcEntity npcEnt = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
            modelNpc.setNpcId(npcEnt.getStringUUID());
            npcEnt.setTradeNpc(modelNpc);
            npcEnt.setPos(pt.getX(), pt.getY(), pt.getZ());
            world.addFreshEntity(npcEnt);

            // 3.d) Ajoute en mémoire
            roadMap.put(pt, npcEnt);
            road.getNpcEntities().add(npcEnt);

            // 3.e) **Persiste la route commerciale** pour enregistrer ce nouveau PNJ
            JsonRepository<CommercialRoad> roadRepo = new JsonRepository<>(
                    Paths.get(OnServerStartedManager.PATH_COMMERCIAL),
                    "roads",
                    json -> CommercialRoad.fromJson(json, world),
                    CommercialRoad::toJson
            );
            // On écrase tout : on enregistre l’ensemble des routes,
            // dont celle-ci, maintenant enrichie de son nouveau PNJ
            roadRepo.saveAll(COMMERCIAL_ROADS);

            LOGGER.info("✅ PNJ '{}' spawné et route '{}' mise à jour dans JSON", npcName, road.getName());
            anySpawned = true;
        }

        if (!anySpawned) {
            LOGGER.info("⚠️ Tous les points sont occupés sur la route '{}'", road.getName());
        }

    }



}
