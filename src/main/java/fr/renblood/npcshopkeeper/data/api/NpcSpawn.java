package fr.renblood.npcshopkeeper.data.api;

import net.minecraft.core.BlockPos;

public class NpcSpawn {
    public String spawn_id;
    public String npc_id; // Référence vers NpcTemplate
    public String world;
    public int x, y, z;
    public float yaw, pitch;
    public String spawn_rule; // STATIC, ROAD, TIMER
    public boolean active;
    
    public BlockPos getPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public String toString() {
        return "NpcSpawn{id='" + spawn_id + "', npc='" + npc_id + "', pos=" + x + "," + y + "," + z + "}";
    }
}
