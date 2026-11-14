package fr.renblood.npcshopkeeper.manager.npc;

import fr.renblood.npcshopkeeper.data.api.NpcSpawn;
import fr.renblood.npcshopkeeper.data.api.NpcTemplate;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration;
import fr.renblood.npcshopkeeper.manager.data.NpcRuntimeSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class GlobalNpcSpawnManager {
    private static final Logger LOGGER = LogManager.getLogger(GlobalNpcSpawnManager.class);
    private static final String SPAWN_ID_TAG = "ApiNpcSpawnId";
    private static final Executor SYNC_EXECUTOR = task -> ForkJoinPool.commonPool().execute(() -> {
        Thread worker = Thread.currentThread();
        ClassLoader previous = worker.getContextClassLoader();
        try {
            // Forge resolves Gson adapter parents through the current thread's loader.
            worker.setContextClassLoader(GlobalNpcSpawnManager.class.getClassLoader());
            task.run();
        } finally {
            worker.setContextClassLoader(previous);
        }
    });
    private static final AtomicBoolean SYNCHRONIZATION_RUNNING = new AtomicBoolean(false);
    private static volatile long lastSyncAt;
    private static volatile long lastSyncDurationMs;
    private static volatile int lastSpawned;
    private static volatile int lastUpdated;
    private static volatile int lastRemoved;
    private static volatile long lastDifferentialRevision;
    private static volatile Map<String, List<NpcSpawn>> lastSpawnsByWorld = Map.of();

    private GlobalNpcSpawnManager() {
    }

    public static void synchronize(MinecraftServer server) {
        List<NpcTemplate> templates = MedievalCoinsIntegration.getNpcs();
        Map<String, List<NpcSpawn>> spawnsByWorld = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            String worldId = level.dimension().location().toString();
            spawnsByWorld.put(worldId, MedievalCoinsIntegration.getNpcSpawns(worldId));
        }
        applySynchronization(server, templates, spawnsByWorld, null);
    }

    public static void synchronizeAsync(MinecraftServer server) {
        if (!SYNCHRONIZATION_RUNNING.compareAndSet(false, true)) return;

        List<String> worldIds = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            worldIds.add(level.dimension().location().toString());
        }

        CompletableFuture.supplyAsync(() -> {
            List<NpcTemplate> templates = MedievalCoinsIntegration.getNpcs();
            Map<String, List<NpcSpawn>> spawnsByWorld = new HashMap<>();
            for (String worldId : worldIds) {
                spawnsByWorld.put(worldId, MedievalCoinsIntegration.getNpcSpawns(worldId));
            }
            return new SynchronizationSnapshot(templates, spawnsByWorld);
        }, SYNC_EXECUTOR).whenComplete((snapshot, error) -> server.execute(() -> {
            try {
                if (error != null) {
                    LOGGER.error("Echec de la synchronisation automatique des PNJ.", error);
                } else if (snapshot != null) {
                    lastSpawnsByWorld = snapshot.spawnsByWorld();
                    applySynchronization(server, snapshot.templates(), snapshot.spawnsByWorld(), null);
                }
            } finally {
                SYNCHRONIZATION_RUNNING.set(false);
            }
        }));
    }

    private static void applySynchronization(
            MinecraftServer server,
            List<NpcTemplate> templates,
            Map<String, List<NpcSpawn>> spawnsByWorld,
            String targetSpawnId) {
        spawnsByWorld = protectPlacements(server,spawnsByWorld);
        lastSpawnsByWorld = spawnsByWorld;
        long startedAt = System.currentTimeMillis();
        templates.forEach(GlobalNpcManager::addNpcFromTemplate);

        int spawned = 0;
        int updated = 0;
        int removed = 0;
        Set<String> disabledIds=templates.stream().filter(t->!t.enabled).map(t->t.npc_id).collect(Collectors.toSet());
        for(TradeNpcEntity entity:NpcEntityIndex.all()){
            if(entity==null||entity.isRemoved())continue;
            TradeNpc model=entity.getTradeNpc();
            if(!entity.getPersistentData().contains(SPAWN_ID_TAG)&&(model==null||!model.isRouteNpc()))continue;
            String profileId=entity.getPersistentData().getString("ApiNpcId");
            if(profileId.isBlank()&&model!=null){
                Map<String,Object> definition=GlobalNpcManager.getNpcData(model.getNpcName());
                if(definition!=null)profileId=String.valueOf(definition.getOrDefault("npcId",""));
            }
            if(disabledIds.contains(profileId)){entity.discard();removed++;}
        }

        for (ServerLevel level : server.getAllLevels()) {
            String worldId = level.dimension().location().toString();
            List<NpcSpawn> spawns = spawnsByWorld.getOrDefault(worldId, List.of());
            if (targetSpawnId != null) {
                spawns = spawns.stream().filter(spawn -> targetSpawnId.equals(spawn.spawn_id)).toList();
            }
            if (spawns.isEmpty() && targetSpawnId == null) {
                LOGGER.warn("Synchronisation PNJ ignoree pour '{}': reponse vide, entites existantes conservees.", worldId);
                continue;
            }
                // La synchronisation ne doit pas charger tous les chunks du monde.
                // Les entités déjà sauvegardées seront restaurées par Minecraft au chargement naturel du chunk.
            Map<String, TradeNpcEntity> existing = getManagedEntities(level);
            if (targetSpawnId != null) {
                TradeNpcEntity target = NpcEntityIndex.getBySpawnId(targetSpawnId);
                existing = target == null || target.level() != level ? Map.of() : Map.of(targetSpawnId, target);
            }
            Set<String> expectedIds = spawns.stream()
                    .filter(GlobalNpcSpawnManager::shouldSpawn)
                    .map(spawn -> spawn.spawn_id)
                    .collect(Collectors.toSet());

            for (Map.Entry<String, TradeNpcEntity> entry : existing.entrySet()) {
                if (!expectedIds.contains(entry.getKey())) {
                    entry.getValue().discard();
                    NpcRuntimeSavedData.get(level).remove(entry.getKey());
                    removed++;
                }
            }

            for (NpcSpawn spawn : spawns) {
                if (!shouldSpawn(spawn)) continue;
                if (!level.hasChunkAt(spawn.getPos())) continue;

                List<TradeNpcEntity> candidates = findSpawnCandidates(level, spawn);
                TradeNpcEntity entity = candidates.stream()
                        .min(Comparator.comparing(candidate -> !candidate.getUUID().equals(stableEntityUuid(spawn.spawn_id))))
                        .orElse(existing.get(spawn.spawn_id));
                UUID stableUuid = stableEntityUuid(spawn.spawn_id);

                // L'index UUID du niveau voit aussi les entites hors de la zone de recherche.
                // Ce controle doit preceder addFreshEntity, sinon Minecraft refuse le doublon.
                Entity entityWithStableUuid = level.getEntity(stableUuid);
                if (entityWithStableUuid instanceof TradeNpcEntity stableEntity && !stableEntity.isRemoved()) {
                    entity = stableEntity;
                } else if (entityWithStableUuid != null && !entityWithStableUuid.isRemoved()) {
                    LOGGER.error("Spawn API '{}' ignore : UUID stable {} deja utilise par {}.",
                            spawn.spawn_id, stableUuid, entityWithStableUuid.getType());
                    continue;
                }

                for (TradeNpcEntity duplicate : candidates) {
                    if (duplicate != entity) {
                        duplicate.discard();
                        removed++;
                    }
                }

                if (entity == null || entity.isRemoved()) {
                    entity = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), level);
                    entity.setUUID(stableUuid);
                    configure(entity, spawn);
                    if (level.addFreshEntity(entity)) {
                        spawned++;
                    } else {
                        LOGGER.error("Impossible de faire apparaitre le spawn API '{}'", spawn.spawn_id);
                    }
                } else {
                    configure(entity, spawn);
                    updated++;
                }
                NpcRuntimeSavedData.get(level).upsert(
                        spawn.spawn_id, spawn.npc_id, worldId, spawn.x, spawn.y, spawn.z, spawn.active);
            }
        }

        LOGGER.info("Synchronisation globale des spawns PNJ terminee : {} crees, {} mis a jour, {} supprimes.",
                spawned, updated, removed);
        lastSyncAt = System.currentTimeMillis();
        lastSyncDurationMs = lastSyncAt - startedAt;
        lastSpawned = spawned;
        lastUpdated = updated;
        lastRemoved = removed;
    }

    public static void synchronizeSpawnAsync(MinecraftServer server, String spawnId) {
        if (spawnId == null || spawnId.isBlank()) return;
        CompletableFuture.supplyAsync(() -> {
            List<NpcTemplate> templates = MedievalCoinsIntegration.getNpcs();
            Map<String, List<NpcSpawn>> spawnsByWorld = new HashMap<>();
            for (ServerLevel level : server.getAllLevels()) {
                spawnsByWorld.put(level.dimension().location().toString(),
                        MedievalCoinsIntegration.getNpcSpawns(level.dimension().location().toString()));
            }
            return new SynchronizationSnapshot(templates, spawnsByWorld);
        }, SYNC_EXECUTOR).whenComplete((snapshot, error) -> server.execute(() -> {
            if (error != null) LOGGER.error("Echec de synchronisation du spawn '{}'.", spawnId, error);
            else applySynchronization(server, snapshot.templates(), snapshot.spawnsByWorld(), spawnId);
        }));
    }

    public static void synchronizeLoadedChunk(ServerLevel level, ChunkPos chunkPos) {
        List<NpcSpawn> worldSpawns = lastSpawnsByWorld.getOrDefault(
            level.dimension().location().toString(), List.of());
        if (worldSpawns.isEmpty()) {
            worldSpawns = NpcRuntimeSavedData.get(level).snapshot().entrySet().stream()
                .map(entry -> savedSpawn(entry.getKey(), entry.getValue()))
                .toList();
        }
        var placements=fr.renblood.npcshopkeeper.manager.data.NpcPlacementSavedData.get(level.getServer());
        if(worldSpawns.isEmpty())worldSpawns=placements.snapshot().stream().filter(s->level.dimension().location().toString().equals(s.world)).toList();
        worldSpawns=worldSpawns.stream().map(placements::reconcile).filter(s->level.dimension().location().toString().equals(s.world)).toList();
        Set<String> expectedIds = worldSpawns.stream()
                .filter(GlobalNpcSpawnManager::shouldSpawn)
                .filter(spawn -> new ChunkPos(spawn.getPos()).equals(chunkPos))
                .map(spawn -> spawn.spawn_id)
                .collect(Collectors.toSet());

        for (TradeNpcEntity entity : NpcEntityIndex.all()) {
            if (entity.level() != level || !new ChunkPos(entity.blockPosition()).equals(chunkPos)) continue;
            String spawnId = entity.getPersistentData().getString(SPAWN_ID_TAG);
            if (!spawnId.isBlank() && !expectedIds.contains(spawnId)) entity.discard();
            if (!spawnId.isBlank() && !expectedIds.contains(spawnId)) {
                NpcRuntimeSavedData.get(level).remove(spawnId);
            }
        }

        for (NpcSpawn spawn : worldSpawns) {
            if (!expectedIds.contains(spawn.spawn_id)) continue;
            UUID stableUuid = stableEntityUuid(spawn.spawn_id);
            Entity existing = level.getEntity(stableUuid);
            if (existing instanceof TradeNpcEntity npc && !npc.isRemoved()) {
                configure(npc, spawn);
                NpcRuntimeSavedData.get(level).upsert(
                        spawn.spawn_id, spawn.npc_id, level.dimension().location().toString(), spawn.x, spawn.y, spawn.z, spawn.active);
                continue;
            }
            TradeNpcEntity entity = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), level);
            entity.setUUID(stableUuid);
            configure(entity, spawn);
            level.addFreshEntity(entity);
                NpcRuntimeSavedData.get(level).upsert(
                    spawn.spawn_id, spawn.npc_id, level.dimension().location().toString(), spawn.x, spawn.y, spawn.z, spawn.active);
        }
    }

    private static Map<String,List<NpcSpawn>> protectPlacements(MinecraftServer server,Map<String,List<NpcSpawn>> incoming) {
        var log=fr.renblood.npcshopkeeper.manager.data.NpcPlacementSavedData.get(server);
        Map<String,List<NpcSpawn>> result=new HashMap<>();
        for(var entries:incoming.values())for(var raw:entries){
            NpcSpawn spawn=log.reconcile(raw);
            result.computeIfAbsent(spawn.world,ignored->new ArrayList<>()).add(spawn);
        }
        return result;
    }

    /** Apply only an API-confirmed edit; profiles, quest IDs and skins are preserved. */
    public static void applyAdminEdit(MinecraftServer server,NpcSpawn spawn) {
        fr.renblood.npcshopkeeper.manager.data.NpcPlacementSavedData.get(server).record(spawn);
        Map<String,List<NpcSpawn>> patched=new HashMap<>();
        lastSpawnsByWorld.forEach((world,rows)->patched.put(world,new ArrayList<>(rows.stream().filter(s->!spawn.spawn_id.equals(s.spawn_id)).toList())));
        patched.computeIfAbsent(spawn.world,ignored->new ArrayList<>()).add(spawn);
        for(var entity:NpcEntityIndex.all())if(entity.level().getServer()==server&&spawn.spawn_id.equals(entity.getApiNpcSpawnId()))entity.discard();
        for(ServerLevel level:server.getAllLevels())NpcRuntimeSavedData.get(level).remove(spawn.spawn_id);
        var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,new net.minecraft.resources.ResourceLocation(spawn.world)));
        if(level!=null&&spawn.active)NpcRuntimeSavedData.get(level).upsert(spawn.spawn_id,spawn.npc_id,spawn.world,spawn.x,spawn.y,spawn.z,true);
        applySynchronization(server,List.of(),patched,spawn.spawn_id);
    }

    public static void synchronizeDifferentialAsync(MinecraftServer server) {
        if (!MedievalCoinsIntegration.supportsDifferentialSync()) {
            synchronizeAsync(server);
            return;
        }
        if (!SYNCHRONIZATION_RUNNING.compareAndSet(false, true)) return;
        CompletableFuture.supplyAsync(() -> MedievalCoinsIntegration.hasNpcChangesSince(lastDifferentialRevision), SYNC_EXECUTOR)
                .whenComplete((changed, error) -> server.execute(() -> {
                    SYNCHRONIZATION_RUNNING.set(false);
                    if (error != null) {
                        LOGGER.error("Echec de verification differentielle des PNJ.", error);
                    } else if (!MedievalCoinsIntegration.supportsDifferentialSync()) {
                        LOGGER.warn("Endpoint differentiel PNJ indisponible; synchronisation complete activee.");
                        synchronizeAsync(server);
                    } else if (Boolean.TRUE.equals(changed)) {
                        lastDifferentialRevision = MedievalCoinsIntegration.getNpcRevision();
                        synchronizeAsync(server);
                    }
                }));
    }

    private record SynchronizationSnapshot(
            List<NpcTemplate> templates,
            Map<String, List<NpcSpawn>> spawnsByWorld) {
    }

    private static boolean shouldSpawn(NpcSpawn spawn) {
        if (spawn == null || !spawn.active || spawn.spawn_id == null || spawn.spawn_id.isBlank()) return false;
        String rule = spawn.spawn_rule == null ? "STATIC" : spawn.spawn_rule.toUpperCase(Locale.ROOT);
        return "STATIC".equals(rule) || "ADMIN".equals(rule);
    }

    private static void configure(TradeNpcEntity entity, NpcSpawn spawn) {
        String npcName = firstNonBlank(spawn.npc_name, GlobalNpcManager.getNpcNameById(spawn.npc_id), spawn.npc_id);
        Map<String, Object> npcData = GlobalNpcManager.getNpcDataById(spawn.npc_id);
        if (npcData == null) {
            npcData = new HashMap<>();
            npcData.put("Texture", firstNonBlank(spawn.npc_skin, "textures/entity/banker.png"));
            npcData.put("Texts", spawn.dialogue != null ? spawn.dialogue : List.of());
            npcData.put("NpcType", firstNonBlank(spawn.npc_type, "DECO"));
            npcData.put("IsShopkeeper", "SHOPKEEPER".equalsIgnoreCase(spawn.npc_type));
        }

        String type = firstNonBlank(spawn.npc_type, String.valueOf(npcData.getOrDefault("NpcType", "DECO")));
        String category = String.valueOf(npcData.getOrDefault("TradeCategory", "api"));
        String entityId = UUID.nameUUIDFromBytes(("npcspawn:" + spawn.spawn_id).getBytes(StandardCharsets.UTF_8)).toString();
        TradeNpc model = new TradeNpc(entityId, npcName, npcData, category, spawn.getPos());

        entity.setTradeNpc(model);
        entity.moveTo(centerBlockCoordinate(spawn.x), spawn.y, centerBlockCoordinate(spawn.z), spawn.yaw, spawn.pitch);
        entity.setYHeadRot(spawn.yaw);
        entity.setApiNpcSpawnId(spawn.spawn_id);
        if (spawn.npc_id != null && !spawn.npc_id.isBlank()) {
            entity.getPersistentData().putString("ApiNpcId", spawn.npc_id);
        }
        entity.getPersistentData().putString("ApiNpcType", type);
        entity.getPersistentData().putBoolean("ApiIsShopkeeper", "SHOPKEEPER".equalsIgnoreCase(type));
        entity.getPersistentData().remove("ApiQuestIds");
    }

    private static List<TradeNpcEntity> findSpawnCandidates(ServerLevel level, NpcSpawn spawn) {
        String expectedName = firstNonBlank(spawn.npc_name, GlobalNpcManager.getNpcNameById(spawn.npc_id), spawn.npc_id);
        UUID stableUuid = stableEntityUuid(spawn.spawn_id);
        AABB nearby = new AABB(
                spawn.x - 1.5D, spawn.y - 1.5D, spawn.z - 1.5D,
                spawn.x + 1.5D, spawn.y + 2.5D, spawn.z + 1.5D
        );

        List<TradeNpcEntity> candidates = new ArrayList<>();
        for (TradeNpcEntity entity : level.getEntitiesOfClass(TradeNpcEntity.class, nearby)) {
            String managedSpawnId = entity.getPersistentData().getString(SPAWN_ID_TAG);
            boolean sameSpawnId = spawn.spawn_id.equals(managedSpawnId);
            boolean stableEntity = stableUuid.equals(entity.getUUID());
            boolean legacyDecorAtSpawn = entity.getPersistentData().getBoolean("IsDecor")
                    && expectedName.equalsIgnoreCase(entity.getNpcName());
            if (sameSpawnId || stableEntity || legacyDecorAtSpawn) {
                candidates.add(entity);
            }
        }
        return candidates;
    }

    private static Map<String, TradeNpcEntity> getManagedEntities(ServerLevel level) {
        return NpcEntityIndex.all().stream()
                .filter(entity -> entity.level() == level && !entity.isRemoved())
                .filter(entity -> entity.getPersistentData().contains(SPAWN_ID_TAG))
                .collect(Collectors.toMap(
                        entity -> entity.getPersistentData().getString(SPAWN_ID_TAG),
                        entity -> entity,
                        (first, duplicate) -> {
                            duplicate.discard();
                            return first;
                        }
                ));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equals(value)) return value;
        }
        return "NPC";
    }

    private static double centerBlockCoordinate(double coordinate) {
        return Math.abs(coordinate - Math.rint(coordinate)) < 0.00001D
                ? coordinate + 0.5D
                : coordinate;
    }

    private static UUID stableEntityUuid(String spawnId) {
        return UUID.nameUUIDFromBytes(("npcspawn-entity:" + spawnId).getBytes(StandardCharsets.UTF_8));
    }

    private static NpcSpawn savedSpawn(String spawnId, NpcRuntimeSavedData.SpawnState state) {
        NpcSpawn spawn = new NpcSpawn();
        spawn.spawn_id = spawnId;
        spawn.npc_id = state.npcId();
        spawn.world = state.world();
        spawn.x = state.x();
        spawn.y = state.y();
        spawn.z = state.z();
        spawn.active = state.active();
        spawn.spawn_rule = "STATIC";
        return spawn;
    }

    private static NpcSpawn localSpawn(MinecraftServer server, String spawnId) {
        TradeNpcEntity entity = NpcEntityIndex.getBySpawnId(spawnId);
        NpcSpawn spawn = new NpcSpawn();
        spawn.spawn_id = spawnId;
        spawn.active = false;
        spawn.spawn_rule = "STATIC";
        if (entity != null) {
            spawn.npc_id = entity.getPersistentData().getString("ApiNpcId");
            spawn.world = entity.level().dimension().location().toString();
            spawn.x = entity.getX();
            spawn.y = entity.getY();
            spawn.z = entity.getZ();
            spawn.yaw = entity.getYRot();
            spawn.pitch = entity.getXRot();
        } else if (server != null) {
            ServerLevel level = server.overworld();
            spawn.world = level.dimension().location().toString();
        }
        return spawn;
    }

    public static boolean shouldRejectJoiningEntity(ServerLevel level, TradeNpcEntity joining) {
        String spawnId = joining.getPersistentData().getString(SPAWN_ID_TAG);
        if (spawnId.isBlank()) return false;
        NpcSpawn placement=fr.renblood.npcshopkeeper.manager.data.NpcPlacementSavedData.get(level.getServer()).find(spawnId);
        if(placement!=null&&(!placement.active||!level.dimension().location().toString().equals(placement.world)
                ||joining.distanceToSqr(centerBlockCoordinate(placement.x),placement.y,centerBlockCoordinate(placement.z))>.01))return true;

        UUID stableUuid = stableEntityUuid(spawnId);
        AABB nearby = joining.getBoundingBox().inflate(2.0D);
        for (TradeNpcEntity existing : level.getEntitiesOfClass(TradeNpcEntity.class, nearby)) {
            if (existing == joining || existing.isRemoved()) continue;
            if (!spawnId.equals(existing.getPersistentData().getString(SPAWN_ID_TAG))) continue;

            if (joining.getUUID().equals(stableUuid)) {
                existing.discard();
                LOGGER.warn("Doublon PNJ API supprime au chargement : {} ({})", existing.getUUID(), spawnId);
                continue;
            }
            LOGGER.warn("Entite PNJ API dupliquee refusee au chargement : {} ({})", joining.getUUID(), spawnId);
            return true;
        }
        return false;
    }

    public static long getLastSyncAt() { return lastSyncAt; }
    public static long getLastSyncDurationMs() { return lastSyncDurationMs; }
    public static int getLastSpawned() { return lastSpawned; }
    public static int getLastUpdated() { return lastUpdated; }
    public static int getLastRemoved() { return lastRemoved; }
    public static boolean isSynchronizationRunning() { return SYNCHRONIZATION_RUNNING.get(); }
}
