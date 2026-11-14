package fr.renblood.npcshopkeeper.manager.data;

import com.google.gson.Gson;
import fr.renblood.npcshopkeeper.data.api.NpcSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Confirmed placement revisions protect against stale HTTP caches and saved entities. */
public final class NpcPlacementSavedData extends SavedData {
    private static final Gson GSON = new Gson();
    private final Map<String,NpcSpawn> edits = new HashMap<>();
    public static NpcPlacementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(NpcPlacementSavedData::load,
                NpcPlacementSavedData::new,"npcshopkeeper_placements");
    }
    public static NpcPlacementSavedData load(CompoundTag tag) {
        var data=new NpcPlacementSavedData();
        CompoundTag entries=tag.getCompound("edits");
        for(String id:entries.getAllKeys()) {
            NpcSpawn spawn=GSON.fromJson(entries.getString(id),NpcSpawn.class);
            if(spawn!=null&&id.equals(spawn.spawn_id))data.edits.put(id,spawn);
        }
        return data;
    }
    public NpcSpawn find(String id) { return edits.get(id); }
    public List<NpcSpawn> snapshot() { return List.copyOf(edits.values()); }
    public void record(NpcSpawn spawn) {
        String json=GSON.toJson(spawn);
        if(!json.equals(GSON.toJson(edits.get(spawn.spawn_id)))) {
            edits.put(spawn.spawn_id,GSON.fromJson(json,NpcSpawn.class));setDirty();
        }
    }
    public NpcSpawn reconcile(NpcSpawn incoming) {
        NpcSpawn latest=edits.get(incoming.spawn_id);
        if(latest==null)return incoming;
        if(revision(incoming)>=revision(latest)) { record(incoming);return incoming; }
        NpcSpawn merged=GSON.fromJson(GSON.toJson(incoming),NpcSpawn.class);
        // A legacy local NBT fallback has no visual profile fields.
        if(merged.npc_skin==null)merged.npc_skin=latest.npc_skin;
        if(merged.npc_name==null)merged.npc_name=latest.npc_name;
        if(merged.npc_type==null)merged.npc_type=latest.npc_type;
        if(merged.dialogue==null)merged.dialogue=latest.dialogue;
        merged.world=latest.world;merged.x=latest.x;merged.y=latest.y;merged.z=latest.z;
        merged.yaw=latest.yaw;merged.pitch=latest.pitch;merged.active=incoming.active&&latest.active;
        merged.meta=merged.meta==null?new HashMap<>():new HashMap<>(merged.meta);
        merged.meta.put("placement_revision",revision(latest));
        return merged;
    }
    public static long revision(NpcSpawn spawn) {
        Object value=spawn.meta==null?null:spawn.meta.get("placement_revision");
        return value instanceof Number number?number.longValue():0;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag entries=new CompoundTag();
        edits.forEach((id,spawn)->entries.putString(id,GSON.toJson(spawn)));
        tag.put("edits",entries);return tag;
    }
}
