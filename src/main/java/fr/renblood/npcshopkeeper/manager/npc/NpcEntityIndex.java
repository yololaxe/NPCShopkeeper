package fr.renblood.npcshopkeeper.manager.npc;

import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.api.NpcShopkeeperAPI;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcEntityIndex {
    private static final Map<UUID, TradeNpcEntity> ENTITIES = new ConcurrentHashMap<>();
    private static final Map<String, UUID> UUID_BY_SPAWN_ID = new ConcurrentHashMap<>();

    private static final Map<InnKey, Map<UUID, TradeNpcEntity>> INN_CUSTOMERS = new ConcurrentHashMap<>();

    private NpcEntityIndex() {}

    private static InnKey innKey(TradeNpcEntity entity) {
        return entity.level() instanceof ServerLevel level && entity.isInnCustomer()
                ? new InnKey(level, entity.getPersistentData().getString(NpcShopkeeperAPI.INN_NAME_TAG)) : null;
    }

    public static List<TradeNpcEntity> innCustomers(ServerLevel level, String innName) {
        var customers = INN_CUSTOMERS.get(new InnKey(level, innName));
        return customers == null ? List.of() : customers.values().stream().filter(npc -> !npc.isRemoved()).toList();
    }

    private record InnKey(ServerLevel level, String name) {}

    public static void add(TradeNpcEntity entity) {
        ENTITIES.put(entity.getUUID(), entity);
        var key = innKey(entity);
        if (key != null) INN_CUSTOMERS.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>()).put(entity.getUUID(), entity);
        String spawnId = entity.getPersistentData().getString("ApiNpcSpawnId");
        if (!spawnId.isBlank()) UUID_BY_SPAWN_ID.put(spawnId, entity.getUUID());
    }

    public static void remove(TradeNpcEntity entity) {
        ENTITIES.remove(entity.getUUID(), entity);
        var key = innKey(entity);
        if (key != null) INN_CUSTOMERS.computeIfPresent(key, (ignored, customers) -> {
            customers.remove(entity.getUUID(), entity);
            return customers.isEmpty() ? null : customers;
        });
        String spawnId = entity.getPersistentData().getString("ApiNpcSpawnId");
        if (!spawnId.isBlank()) UUID_BY_SPAWN_ID.remove(spawnId, entity.getUUID());
    }

    public static TradeNpcEntity get(UUID uuid) {
        TradeNpcEntity entity = ENTITIES.get(uuid);
        return entity != null && !entity.isRemoved() ? entity : null;
    }

    public static TradeNpcEntity getBySpawnId(String spawnId) {
        UUID uuid = UUID_BY_SPAWN_ID.get(spawnId);
        return uuid == null ? null : get(uuid);
    }

    public static Collection<TradeNpcEntity> all() { return List.copyOf(ENTITIES.values()); }
    public static int size() { return ENTITIES.size(); }
    public static int apiSize() { return UUID_BY_SPAWN_ID.size(); }
}
