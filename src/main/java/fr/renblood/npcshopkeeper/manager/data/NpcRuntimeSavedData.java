package fr.renblood.npcshopkeeper.manager.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class NpcRuntimeSavedData extends SavedData {
    private static final String DATA_NAME = "npcshopkeeper_runtime";
    private final Map<String, SpawnState> spawns = new HashMap<>();

    public static NpcRuntimeSavedData load(CompoundTag tag) {
        NpcRuntimeSavedData data = new NpcRuntimeSavedData();
        CompoundTag entries = tag.getCompound("spawns");
        for (String spawnId : entries.getAllKeys()) {
            CompoundTag entry = entries.getCompound(spawnId);
            data.spawns.put(spawnId, new SpawnState(
                    entry.getString("npc_id"),
                    entry.getString("world"),
                    entry.getDouble("x"),
                    entry.getDouble("y"),
                    entry.getDouble("z"),
                    entry.getBoolean("active")));
        }
        return data;
    }

    public static NpcRuntimeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            NpcRuntimeSavedData::load,
            NpcRuntimeSavedData::new,
                DATA_NAME);
    }

    public void upsert(String spawnId, String npcId, String world, double x, double y, double z, boolean active) {
        if (spawnId == null || spawnId.isBlank()) return;
        spawns.put(spawnId, new SpawnState(npcId, world, x, y, z, active));
        setDirty();
    }

    public void remove(String spawnId) {
        if (spawns.remove(spawnId) != null) setDirty();
    }

    public Map<String, SpawnState> snapshot() {
        return Collections.unmodifiableMap(spawns);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag entries = new CompoundTag();
        spawns.forEach((spawnId, state) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("npc_id", state.npcId());
            entry.putString("world", state.world());
            entry.putDouble("x", state.x());
            entry.putDouble("y", state.y());
            entry.putDouble("z", state.z());
            entry.putBoolean("active", state.active());
            entries.put(spawnId, entry);
        });
        tag.put("spawns", entries);
        return tag;
    }

    public record SpawnState(String npcId, String world, double x, double y, double z, boolean active) {
    }
}
