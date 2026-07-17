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
import fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache;
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

    public enum SpawnResult {
        SPAWNED,
        FULL,
        NO_NPC_AVAILABLE,
        NO_FREE_POINT
    }

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

    public static SpawnResult trySpawnNpcForRoad(ServerLevel world, CommercialRoad road) {
        // Récupère (ou crée) la map des PNJs actifs pour cette route
        var roadMap = activeNPCs.computeIfAbsent(road, r -> new HashMap<>());

        // 1) Nettoyage des PNJs invalides (morts ou supprimés) AVANT de vérifier la capacité
        roadMap.entrySet().removeIf(e -> {
            Mob mob = e.getValue();
            if (!(mob instanceof TradeNpcEntity) || mob.isRemoved()) {
                // Si l'entité est supprimée, on doit libérer le nom du PNJ
                if (mob instanceof TradeNpcEntity npcEntity) {
                    String npcName = npcEntity.getNpcName();
                    if (npcName != null) {
                        GlobalNpcManager.deactivateNpc(npcName);
                        LOGGER.info("♻️ PNJ '{}' libéré (mort/despawn) sur la route '{}'", npcName, road.getName());
                    }
                    // Retirer aussi de la liste persistante de la route
                    road.getNpcEntities().remove(npcEntity);
                }
                return true; // Supprimer de la map active
            }
            return false;
        });

        // 2) Vérification de la capacité
        int occupied = roadMap.size();
        int capacity = road.getPositions().size();
        if (occupied >= capacity) {
            LOGGER.info("Route '{}' pleine ({}/{}), prochain controle repousse.", road.getName(), occupied, capacity);
            return SpawnResult.FULL;
        }

        // 3) On cherche le premier point libre pour spawn
        for (BlockPos pt : road.getPositions()) {
            if (roadMap.containsKey(pt)) continue;

            LOGGER.info("🧭 Spawn NPC sur route '{}' au point {}", road.getName(), pt);

            // ── 3.1) Choix d'un PNJ inactif
            String npcName = GlobalNpcManager.getRandomInactiveNpc();
            if (npcName == null) {
                LOGGER.error("❌ Aucun PNJ inactif disponible pour la route '{}'", road.getName());
                return SpawnResult.NO_NPC_AVAILABLE;
            }
            var npcData = GlobalNpcManager.getNpcData(npcName);

            // ── 3.2) Création du modèle TradeNpc
            TradeNpc modelNpc = new TradeNpc(npcName, npcData, road.getCategory(), pt);
            modelNpc.setRouteNpc(true);

            // ── 3.3) **Chargement et assignation d'un Trade**
            List<Trade> available = RuntimeDataCache.getTradesByCategory(modelNpc.getTradeCategory());
            if (!available.isEmpty()) {
                Trade chosen = available.get(random.nextInt(available.size()));
                modelNpc.setTrade(chosen);
                LOGGER.info("✅ Trade '{}' assigné à PNJ '{}'", chosen.getName(), npcName);
            } else {
                LOGGER.warn("⚠️ Aucun trade disponible pour la catégorie '{}'", modelNpc.getTradeCategory());
            }

            // ── 3.4) Création et initialisation de l'entité
            TradeNpcEntity npcEnt = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
            modelNpc.setNpcId(npcEnt.getStringUUID());
            npcEnt.setTradeNpc(modelNpc);
            npcEnt.setPos(pt.getX() + 0.5D, pt.getY(), pt.getZ() + 0.5D);
            world.addFreshEntity(npcEnt);

            // … après world.addFreshEntity(npcEnt);
            roadMap.put(pt, npcEnt);
// au lieu de saveAll(COMMERCIAL_ROADS) :
            road.addNpcAndPersist(npcEnt, COMMERCIAL_ROADS);
            LOGGER.info("💾 PNJ '{}' ajouté et route '{}' mise à jour", npcName, road.getName());

            // Marquer le PNJ comme actif pour qu'il ne soit plus choisi
            GlobalNpcManager.activateNpc(modelNpc);

            // ── 3.7) Persistance du nouveau PNJ dans trades_npcs.json
            JsonRepository<TradeNpc> npcRepo = new JsonRepository<>(
                    Paths.get(OnServerStartedManager.PATH_NPCS),
                    "npcs",
                    TradeNpc::fromJson,
                    TradeNpc::toJson
            );
            npcRepo.add(modelNpc);
            LOGGER.info("💾 PNJ '{}' enregistré dans '{}'", npcName, OnServerStartedManager.PATH_NPCS);

            // Un seul PNJ spawné à la volée, on stoppe la boucle
            return SpawnResult.SPAWNED;
        }

        LOGGER.info("⚠️ Tous les points sont occupés sur la route '{}'", road.getName());
        return SpawnResult.NO_FREE_POINT;
    }
}
