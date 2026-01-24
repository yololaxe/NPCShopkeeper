package fr.renblood.npcshopkeeper.data.harbor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

public class Port {
    private String name;
    private int x, y, z;
    private String dimension; // Pour éviter de tp dans le vide si on change de dimension

    public Port(String name, BlockPos pos, String dimension) {
        this.name = name;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.dimension = dimension;
    }

    public String getName() { return name; }
    public BlockPos getPos() { return new BlockPos(x, y, z); }
    public String getDimension() { return dimension; }

    public static Port fromJson(JsonObject json) {
        return new Gson().fromJson(json, Port.class);
    }

    public JsonObject toJson() {
        return (JsonObject) new Gson().toJsonTree(this);
    }
}
