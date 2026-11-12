package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
public final class VoyageData extends SavedData {
    public JsonObject tickets=new JsonObject();
    public static VoyageData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(VoyageData::load,VoyageData::new,"renblood_passages_v1");}
    public static VoyageData load(CompoundTag tag){VoyageData d=new VoyageData();if(tag.contains("tickets"))d.tickets=JsonParser.parseString(tag.getString("tickets")).getAsJsonObject();return d;}
    @Override public CompoundTag save(CompoundTag tag){tag.putString("tickets",tickets.toString());return tag;}
    public void persist(MinecraftServer server){setDirty();server.overworld().getDataStorage().save();}
}
