package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class RealmAtlas {
    private static final JsonArray CITIES=load();
    private static JsonArray load(){try(var stream=RealmAtlas.class.getResourceAsStream("/assets/npcshopkeeper/atlas.json")){if(stream==null)return new JsonArray();return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray();}catch(Exception e){throw new IllegalStateException("Atlas de Renblood illisible",e);}}
    public static JsonArray cities(){return CITIES;}
    private static JsonObject city(String name){for(var v:CITIES){var c=v.getAsJsonObject();if(c.get("name").getAsString().equalsIgnoreCase(name))return c;}return null;}
    public static String county(String name){var c=city(name);return c==null?"Non rattaché":c.get("county").getAsString();}
    public static double coordinate(String name,String axis){var c=city(name);return c==null?-1:c.get(axis).getAsDouble();}
}
