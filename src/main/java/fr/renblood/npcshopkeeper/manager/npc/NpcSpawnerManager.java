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

    public static void trySpawnNpcForRoad(ServerLevel world, CommercialRoad road) {
        var roadMap = activeNPCs.computeIfAbsent(road, r -> new HashMap<>());
        // 1) on vide les entrées invalides
        roadMap.entrySet().removeIf(e ->
                !(e.getValue() instanceof TradeNpcEntity) || e.getValue().isRemoved()
        );

        // 2) on cherche le premier point libre
        for (BlockPos pt : road.getPositions()) {
            if (roadMap.containsKey(pt)) continue;

            LOGGER.info("🧭 Spawn NPC sur route '{}' au point {}", road.getName(), pt);

            // ── On récupère un PNJ inactif et ses datas ─────────────────────
            String npcName = GlobalNpcManager.getRandomInactiveNpc();
            if (npcName == null) {
                LOGGER.error("❌ Aucun PNJ inactif disponible.");
                return;
            }
            var npcData = GlobalNpcManager.getNpcData(npcName);

            // ── On crée le modèle et l’entité ───────────────────────────────
            TradeNpc modelNpc = new TradeNpc(npcName, npcData, road.getCategory(), pt);
            TradeNpcEntity npcEnt = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);

            // IMPORTANT : on donne l’UUID au modèle avant d’initialiser l’entité
            modelNpc.setNpcId(npcEnt.getStringUUID());
            // initialise texture, nom, position, etc.
            npcEnt.setTradeNpc(modelNpc);
            npcEnt.setPos(pt.getX(), pt.getY(), pt.getZ());

            // 3) on l’ajoute au monde et à nos maps
            world.addFreshEntity(npcEnt);
            roadMap.put(pt, npcEnt);
            road.getNpcEntities().add(npcEnt);

            // 4) on persiste la nouvelle route + le nouveau PNJ (si tu veux)
            // … JsonRepository.saveAll / .add comme tu as configuré

            LOGGER.info("✅ PNJ spawné sur la route '{}' au point {} : {}", road.getName(), pt, npcName);
            return;  // un seul spawn par tick
        }

        LOGGER.info("⚠️ Tous les points sont occupés sur la route '{}'", road.getName());
    }

}
