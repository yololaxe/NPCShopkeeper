package fr.renblood.npcshopkeeper.ambience;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Client preferences, loaded once; never reads an API or touches NPC save data. */
public final class NpcAmbientSettings {
    public boolean enabled=true;
    public boolean greetings=true;
    public float volume=.22f;
    public double radius=12;
    public int minDelaySeconds=12;
    public int maxDelaySeconds=28;
    private static NpcAmbientSettings current;
    public NpcAmbientSettings sanitized() {
        volume=Float.isFinite(volume)?Math.max(0,Math.min(.6f,volume)):.22f;
        radius=Double.isFinite(radius)?Math.max(4,Math.min(24,radius)):12;
        minDelaySeconds=Math.max(6,Math.min(120,minDelaySeconds));maxDelaySeconds=Math.max(minDelaySeconds,Math.min(180,maxDelaySeconds));return this;
    }
    public static NpcAmbientSettings get() {
        if(current!=null)return current;
        var file=FMLPaths.CONFIGDIR.get().resolve("npcshopkeeper/ambience_client.json");Gson gson=new GsonBuilder().setPrettyPrinting().create();
        try {
            if(Files.exists(file)) {current=gson.fromJson(Files.readString(file,StandardCharsets.UTF_8),NpcAmbientSettings.class);if(current==null)throw new IllegalStateException("Empty settings");}
            else {current=new NpcAmbientSettings();Files.createDirectories(file.getParent());Files.writeString(file,gson.toJson(current),StandardCharsets.UTF_8);}
            current.sanitized();
        }catch(Exception failure){current=new NpcAmbientSettings();current.enabled=false;org.apache.logging.log4j.LogManager.getLogger().warn("Ambiance PNJ désactivée : configuration client illisible ; fichier conservé.");}
        return current;
    }
    public static boolean audible(boolean alive,boolean silent,boolean invisible,boolean sleeping,double distanceSquared,double radius) {
        return alive&&!silent&&!invisible&&!sleeping&&Double.isFinite(distanceSquared)&&distanceSquared>=0&&distanceSquared<=radius*radius;
    }
}
