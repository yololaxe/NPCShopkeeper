package fr.renblood.npcshopkeeper.data.api;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

public class NpcSpawn {
    public String spawn_id;
    public String npc_id;
    public String world;
    public double x, y, z;
    public float yaw, pitch;
    public String spawn_rule;
    public boolean active;

    // Optional fields returned by an enriched world-spawn endpoint.
    public String npc_name;
    public String npc_type;
    public String npc_skin;
    public List<String> dialogue;
    public List<String> quest_ids;
    public Map<String, Object> meta;

    public BlockPos getPos() {
        return BlockPos.containing(x, y, z);
    }

    @Override
    public String toString() {
        return "NpcSpawn{id='" + spawn_id + "', npc='" + npc_id + "', pos=" + x + "," + y + "," + z + "}";
    }
}
